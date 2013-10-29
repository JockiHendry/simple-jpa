package simplejpa

import org.codehaus.groovy.runtime.metaclass.ConcurrentReaderHashMap
import org.slf4j.*
import simplejpa.transaction.EntityManagerLifespan
import simplejpa.transaction.ReturnFailedSignal
import simplejpa.transaction.TransactionHolder

import javax.persistence.*
import griffon.util.*

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.ParameterExpression
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import javax.validation.ConstraintViolation
import javax.validation.Validator
import javax.validation.groups.Default
import java.lang.reflect.Method

final class SimpleJpaHandler {

    private static Logger LOG = LoggerFactory.getLogger(SimpleJpaHandler)

    private static final PATTERN_FINDALLMODEL = /findAll([A-Z]\w*)/
    private static final PATTERN_FINDMODELBYID_NOTSOFTDELETED = /find([A-Z]\w*)ByIdNotSoftDeleted/
    private static final PATTERN_FINDMODELBYID = /find([A-Z]\w*)ById/
    private static final PATTERN_FINDMODELBYDSL = /find(All)?([A-Z]\w*)ByDsl/
    private static final PATTERN_FINDMODELBYATTRIBUTE = /find(All)?([A-Z]\w*)By([A-Z]\w*)/
    private static final PATTERN_FINDMODELBY = /find(All)?([A-Z]\w*)By(And|Or)/
    private static final PATTERN_DONAMEDQUERY = /do([A-Z]\w*)On([A-Z]\w*)/
    private static final PATTERN_SOFTDELETE = /softDelete([A-Z]\w*)/

    private static final int DEFAULT_PAGE_SIZE = 10

    final String prefix
    final String domainClassPackage
    final EntityManagerFactory emf
    final Validator validator
    final boolean alwaysExcludeSoftDeleted
    final EntityManagerLifespan entityManagerLifespan
    final boolean isCheckThreadSafeLoading
    final FlushModeType defaultFlushMode



    private boolean convertEmptyStringToNull
    final ConcurrentReaderHashMap mapTransactionHolder = new ConcurrentReaderHashMap()

    public SimpleJpaHandler(EntityManagerFactory emf, Validator validator) {

        this.emf = emf
        this.validator = validator

        //
        // Initialize fields from related configurations
        //
        griffon.core.GriffonApplication app = griffon.util.ApplicationHolder.application
        this.entityManagerLifespan = ConfigUtils.getConfigValue(app.config,
            'griffon.simplejpa.entityManager.lifespan', 'MANUAL').toUpperCase()
        this.defaultFlushMode = ConfigUtils.getConfigValue(app.config,
            'griffon.simplejpa.entityManager.defaultFlushMode', 'AUTO').toUpperCase()
        this.isCheckThreadSafeLoading = ConfigUtils.getConfigValueAsBoolean(app.config,
            'config.griffon.simplejpa.entityManager.checkThreadSafeLoading', false)
        this.prefix = ConfigUtils.getConfigValueAsString(app.config, 'griffon.simplejpa.method.prefix', '')
        this.domainClassPackage = ConfigUtils.getConfigValueAsString(app.config, 'griffon.simplejpa.domain.package', 'domain')
        this.alwaysExcludeSoftDeleted = ConfigUtils.getConfigValueAsBoolean(app.config,
            'griffon.simplejpa.finder.alwaysExcludeSoftDeleted', false)
        this.convertEmptyStringToNull = ConfigUtils.getConfigValueAsBoolean(app.config,
            'griffon.simplejpa.validation.convertEmptyStringToNull', false)

        if (LOG.isDebugEnabled()) {
            LOG.debug "SimpleJpaHandler initializd with the following configuration: \n" +
                "griffon.simplejpa.entityManager.lifespan = $entityManagerLifespan\n" +
                "griffon.simplejpa.entityManager.defaultFlushMode = $defaultFlushMode\n" +
                "griffon.simplejpa.entityManager.checkThreadSafeLoading = $isCheckThreadSafeLoading\n" +
                "griffon.simplejpa.method.prefix = $prefix\n" +
                "griffon.simplejpa.domain.package = $domainClassPackage\n" +
                "griffon.simplejpa.finder.alwaysExcludeSoftDeleted = $alwaysExcludeSoftDeleted\n" +
                "griffon.simplejpa.validation.convertEmptyStringToNull = $convertEmptyStringToNull\n"
        }

    }

    private void debugEntityManager() {
        if (LOG.isInfoEnabled()) {
            def result = mapTransactionHolder.collect{k,v -> "${k.id}=$v"}.join('\n')
            LOG.info "List of cached EntityManager:\n$result"
        }
    }

    def getEntityManager = {
        LOG.info "Retrieving current EntityManager from thread ${Thread.currentThread().id}..."
        EntityManager em = mapTransactionHolder.get(Thread.currentThread())?.em
        debugEntityManager()
        em
    }

    def createEntityManager = { TransactionHolder copy = null ->
        TransactionHolder th
        if (copy) {
            LOG.info "Creating a new entity manager based on $copy..."
            th = new TransactionHolder(emf.createEntityManager(), copy)
        } else {
            LOG.info "Creating a new entity manager..."
            EntityManager em = emf.createEntityManager()
            em.setFlushMode(defaultFlushMode)
            th = new TransactionHolder(em)
        }
        mapTransactionHolder.put(Thread.currentThread(), th)
        debugEntityManager()
        ApplicationHolder.application.event("simpleJpaCreateEntityManager", [th])
        th
    }

    def destroyEntityManager = {
        LOG.info "Destroying all entity managers..."
        mapTransactionHolder.each { Thread k, TransactionHolder v ->
            if (v.em.isOpen()) {
                v.em.close()
            }
        }
        mapTransactionHolder.clear()
        debugEntityManager()
        ApplicationHolder.application.event("simpleJpaDestroyEntityManagers")
    }

    private configureCriteria(CriteriaBuilder cb, CriteriaQuery c, Root model, Map config) {
        LOG.info "Processing configuration [$config]..."

        Predicate p = c.getRestriction()?: cb.conjunction()

        if (alwaysExcludeSoftDeleted || config["notSoftDeleted"]==true) {
            LOG.info "Applying not soft deleted..."
            p = cb.and(p, cb.equal(model.get("deleted"), "N"))
            c.where(p)
        }
        if (config['excludeSubclass']!=null) {
            String excludeSubclass = config['excludeSubclass']
            if (excludeSubclass=='*') {
                LOG.info "Exclude all subclasses..."
                p = cb.and(p, cb.equal(model.type(), cb.literal(model.model.javaType)))
                c.where(p)
            } else {
                excludeSubclass.split(',').each {
                    Class subclass = Class.forName("${domainClassPackage}.${it.trim()}")
                    LOG.info "Exclude subclass: ${subclass}..."
                    p = cb.and(p, cb.notEqual(model.type(), cb.literal(subclass)))
                }
                c.where(p)
            }
        }

        if (config["orderBy"]!=null) {
            List orders = []
            List orderBy = config["orderBy"].tokenize(',')
            List orderDirection = config["orderDirection"]?.tokenize(',')

            orderBy.eachWithIndex { String fieldName, int index ->
                String direction = orderDirection?.get(index) ?: "asc"
                orders << cb."$direction"(model.get(fieldName))
            }

            LOG.info "Applying order by [$orders]..."
            if (orders.size() > 0) c.orderBy(orders)
        }
    }

    private Query configureQuery(Query query, Map config) {
        if (config["page"]!=null || config["pageSize"]!=null) {
            int page = config["page"] as Integer ?: 1
            page = (page - 1) >= 0 ? (page-1) : 0
            int pageSize = config["pageSize"] as Integer ?: DEFAULT_PAGE_SIZE
            query.setFirstResult(page*pageSize)
            query.setMaxResults(pageSize)
        }
        if (config['flushMode']) {
            query.setFlushMode(config['flushMode'])
        }
        query
    }

    private def filterResult(List results, boolean returnAll) {
        if (!returnAll) {
            if (results.isEmpty()) return null
            return results[0]
        }
        results
    }

    def beginTransaction = { boolean resume = true, boolean newSession = false ->
        LOG.info "Begin transaction from thread ${Thread.currentThread().id} (resume=$resume) (newSession=$newSession)..."
        if (newSession) {
            LOG.info "Start a new session..."
            destroyEntityManager()
        }

        TransactionHolder th
        if (entityManagerLifespan==EntityManagerLifespan.TRANSACTION) {
            // always create new EntityManager for each transaction
            th = createEntityManager()
        } else if (entityManagerLifespan==EntityManagerLifespan.MANUAL) {
            // reuse previous EntityManager if possible
            th = mapTransactionHolder.get(Thread.currentThread())
            if (!th) {
                th = createEntityManager()
            }
        }
        if (th.beginTransaction(resume)) {
            ApplicationHolder.application.event("simpleJpaNewTransaction", [th])
        }
    }

    def closeAndRemoveCurrentEntityManager = {
        TransactionHolder th = mapTransactionHolder.get(Thread.currentThread())
        if (th) {
            ApplicationHolder.application.event("simpleJpaBeforeCloseEntityManager", [th])
            th.em.close()
            LOG.info "EntityManager for $th is closed!"
            mapTransactionHolder.remove(Thread.currentThread())
            LOG.info "EntityManager for thread ${Thread.currentThread()} is removed!"
        }
    }

    def commitTransaction = {
        LOG.info "Commit transaction from thread ${Thread.currentThread()} (${Thread.currentThread().id})..."
        TransactionHolder th = mapTransactionHolder.get(Thread.currentThread())
        if (th?.commitTransaction()) {
            if (entityManagerLifespan==EntityManagerLifespan.TRANSACTION) {
                closeAndRemoveCurrentEntityManager()
            }
            ApplicationHolder.application.event("simpleJpaCommitTransaction", [th])
        }
    }

    def rollbackTransaction = {
        LOG.info "Rollback transaction from thread ${Thread.currentThread()} (${Thread.currentThread().id})..."
        TransactionHolder th = mapTransactionHolder.get(Thread.currentThread())
        th.em.clear()
        if (th.rollbackTransaction()) {
            closeAndRemoveCurrentEntityManager()
            if (entityManagerLifespan==EntityManagerLifespan.MANUAL) {
                createEntityManager(th)
            }
            ApplicationHolder.application.event("simpleJpaRollbackTransaction", [th])
        }
    }

    def executeInsideTransaction(Closure action) {
        boolean insideTransaction = true
        boolean isError = false
        def result
        if (!getEntityManager()?.transaction?.isActive()) {
            insideTransaction = false
            ApplicationHolder.application.event("simpleJpaBeforeAutoCreateTransaction")
            beginTransaction()
        }
        LOG.info "Not in a transaction? ${!insideTransaction}"
        try {
            result = action()
        } catch (Exception ex) {
            LOG.error "Error when not in a transaction? ${!insideTransaction}", ex
            if (!insideTransaction) {
                isError = true
                rollbackTransaction()
            }
            throw new Exception(ex)
        } finally {
            if (!insideTransaction && !isError) {
                commitTransaction()
            }
        }
        LOG.info "Transaction is done!"
        return result
    }

    def returnFailed = {
        throw new ReturnFailedSignal()
    }

    def executeQuery = { String jpql, Map config = [:] ->
        LOG.info "Executing query $jpql"
        executeInsideTransaction {
            configureQuery(getEntityManager().createQuery(jpql), config).getResultList()
        }
    }

    def executeNativeQuery = { String sql, Map config = [:] ->
        LOG.info "Executing native query $sql"
        executeInsideTransaction {
            configureQuery(getEntityManager().createNativeQuery(sql), config).getResultList()
        }
    }

    def findAllModel = { String model ->
        return { Map config = [:] ->
            LOG.info "Executing findAll$model"
            executeInsideTransaction {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder()
                CriteriaQuery c = cb.createQuery()
                Root rootModel = c.from(Class.forName(domainClassPackage + "." + model))
                c.select(rootModel)

                configureCriteria(cb, c, rootModel, config)
                configureQuery(getEntityManager().createQuery(c), config).getResultList()
            }
        }
    }

    def findModelById = { String model, boolean notSoftDeleted ->
        def modelClass = Class.forName(domainClassPackage + "." + model)

        return { id ->
            LOG.info "Executing find$model for class $modelClass and id [$id]"
            executeInsideTransaction {
                def idClass = getEntityManager().metamodel.entity(modelClass).idType.javaType
                Object object = getEntityManager().find(modelClass, idClass.newInstance(id))
                if (notSoftDeleted) {
                    if (object."deleted"=="Y") return null
                }
                object
            }
        }
    }

    def findByDsl = { Class modelClass, Map config = [:], Closure closure ->
        findModelByDsl(modelClass, false, config, closure)
    }

    def findAllByDsl = { Class modelClass, Map config = [:], Closure closure ->
        findModelByDsl(modelClass, true, config, closure)
    }

    def findModelByDsl = { Class modelClass, boolean returnAll, Map config = [:], Closure closure ->
        LOG.info "Find entities by Dsl: model=$modelClass, returnAll=$returnAll, config=$config"
        executeInsideTransaction {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder()
            CriteriaQuery c = cb.createQuery()
            Root rootModel = c.from(modelClass)
            c.select(rootModel)

            closure.setResolveStrategy(Closure.DELEGATE_FIRST)
            closure.delegate = new QueryDsl(cb: cb, rootModel: rootModel)
            closure.call()
            c.where(closure.delegate.criteria)

            configureCriteria(cb, c, rootModel, config)
            filterResult(configureQuery(getEntityManager().createQuery(c), config).resultList, returnAll)
        }
    }

    def findModelBy = { Class modelClass, boolean returnAll, boolean isAnd = true, Map args, Map config = [:] ->
        LOG.info "Find entities by: model=$modelClass, returnAll=$returnAll, args=$args, confing=$config"
        executeInsideTransaction {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder()
            CriteriaQuery c = cb.createQuery()
            Root rootModel = c.from(modelClass)
            c.select(rootModel)

            Predicate criteria
            if (isAnd) {
                criteria = cb.conjunction()
                args.each { key, value ->
                    ParameterExpression p = cb.parameter(rootModel.get(key).javaType, key)
                    criteria = cb.and(criteria, cb.equal(rootModel.get(key), p))
                }
            } else {
                criteria = cb.disjunction()
                args.each { key, value ->
                    ParameterExpression p = cb.parameter(rootModel.get(key).javaType, key)
                    criteria = cb.or(criteria, cb.equal(rootModel.get(key), p))
                }
            }

            c.where(criteria)
            configureCriteria(cb, c, rootModel, config)
            Query q = configureQuery(getEntityManager().createQuery(c), config)
            args.each { key, value -> q.setParameter(key, value)}
            filterResult(q.getResultList(), returnAll)
        }
    }

    def findByAnd = { Class modelClass, Map args, Map config = [:] ->
        findModelBy(modelClass, false, true, args, config)
    }

    def findAllByAnd = { Class modelClass, Map args, Map config = [:] ->
        findModelBy(modelClass, true, true, args, config)
    }

    def findByOr = { Class modelClass, Map args, Map config = [:] ->
        findModelBy(modelClass, false, false, args, config)
    }

    def findAllByOr = { Class modelClass, Map args, Map config = [:] ->
        findModelBy(modelClass, true, false, args, config)
    }

    public def parseFinder(String finder) {
        LOG.info "Parsing $finder"
        List results = []
        finder.split('(And|Or)').each { String expr ->
            LOG.info "Expression: $expr"
            int start = finder.indexOf(expr)
            int operStart
            String operName, fieldName
            int argsCount
            def availableOperators = QueryDsl.OPERATORS.keySet().toArray()
            for (int i=0; i<availableOperators.size(); i++) {
                operStart = expr.toLowerCase().lastIndexOf(availableOperators[i].toLowerCase())
                if (operStart > -1) {
                    operName = QueryDsl.OPERATORS[availableOperators[i]].operation
                    argsCount = QueryDsl.OPERATORS[availableOperators[i]].argsCount
                    break
                }
            }
            if (operStart <= 0) {
                operName = 'equal'
                fieldName = GriffonNameUtils.uncapitalize(expr)
                argsCount = 1
            } else {
                fieldName = GriffonNameUtils.uncapitalize(expr.substring(0, operStart))
            }

            def whereExpr = [field: fieldName, oper: operName, argsCount: argsCount, isAnd: null, isOr: null]
            if (start > 0) {
                if (finder.substring(0, start).endsWith('And')) {
                    whereExpr.isAnd = true
                } else if (finder.substring(0, start).endsWith('Or')) {
                    whereExpr.isOr = true
                }
            }
            LOG.info "Result: $whereExpr"
            results << whereExpr
        }
        results
    }

    def findModelByAttribute = { Class modelClass, boolean returnAll, List whereExprs, Object[] args ->
        def config = [:]
        List argsList = args.toList()
        if (args.length > 0 && args.last() instanceof Map) {
            config = args.last()
            argsList.pop()
        }
        LOG.info "Find entities by attribute: model=$modelClass, returnAll=$returnAll, whereExprs=$whereExprs, args=$args, confing=$config"
        executeInsideTransaction {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder()
            CriteriaQuery c = cb.createQuery()
            Root rootModel = c.from(modelClass)
            c.select(rootModel)

            Predicate p
            List params = []

            if (whereExprs.size()==1) {
                def arguments = [rootModel.get(whereExprs[0].field)]
                if (whereExprs[0].argsCount > 0) {
                    (0..whereExprs[0].argsCount-1).each {
                        Parameter param = cb.parameter(rootModel.get(whereExprs[0].field).javaType)
                        arguments << param
                        params << param
                    }
                }
                p = cb."${whereExprs[0].oper}"(*arguments)
                c.where(p)
            } else {
                whereExprs.each { expr ->
                    def arguments = [rootModel.get(expr.field)]
                    if (expr.argsCount > 0) {
                        (0..expr.argsCount-1).each {
                            Parameter param = cb.parameter(rootModel.get(expr.field).javaType)
                            arguments << param
                            params << param
                        }
                    }
                    Predicate p2 = cb."${expr.oper}"(*arguments)
                    if (expr.isAnd) {
                        p = cb.and(p, p2)
                    } else if (expr.isOr) {
                        p = cb.or(p, p2)
                    } else {
                        p = p2
                    }
                }
                c.where(p)
            }

            configureCriteria(cb, c, rootModel, config)
            Query q = configureQuery(getEntityManager().createQuery(c), config)
            argsList.each{ q.setParameter(params.remove(0), it)}
            filterResult(q.resultList, returnAll)
        }
    }

    def doNamedQuery = { String namedQuery, String model ->
        return { Map args, Map config = [:] ->
            LOG.info "Executing named query [${model}.${namedQuery}] with argument [$args]"
            executeInsideTransaction {
                Query query = getEntityManager().createNamedQuery("${model}.${namedQuery}")
                args.each { key, value ->
                    if (query.parameters.find { it.name == key }) {
                        query.setParameter(key, value)
                    }
                }

                configureQuery(query, config).getResultList()
            }
        }
    }

    def softDeleteModel = { String model ->
        return { id ->
            LOG.info "Executing soft delete for [$model] with id [$id]"
            executeInsideTransaction {
                def object = findModelById(model, false).call(id)
                object."deleted" = "Y"
            }
        }
    }

    def softDelete = { model ->
        LOG.info "Executing softDelete for [$model]"
        executeInsideTransaction {
            EntityManager em = getEntityManager()
            if (!em.contains(model)) {
                model = em.merge(model)
            }
            model.deleted = "Y"
        }
    }

    def persist = { model ->
        LOG.info "Executing persist for [$model]"
        executeInsideTransaction {
            EntityManager em = getEntityManager()
            em.persist(model)
        }
    }

    def merge = { model ->
        LOG.info "Executing merge for [$model]"
        executeInsideTransaction {
            EntityManager em = getEntityManager()
            return em.merge(model)
        }
    }

    def remove = { model ->
        LOG.info "Executing remove for [$model]"
        executeInsideTransaction {
            def persistedModel = model
            EntityManager em = getEntityManager()
            if (!em.contains(model)) {
                persistedModel = em.find(model.class, model.id)
                if (!persistedModel) {
                    persistedModel = em.merge(model)
                }
            }
            em.remove(persistedModel)
        }
    }

    def validate = { model, group = Default ->
        LOG.info "Validating model [$model] group [$group]"

        // Make sure no existing errors before validating
        def viewModel = delegate.model
        if (viewModel.hasError()) return false

        // Convert empty string to null if required
        if (convertEmptyStringToNull) {
            model.properties.each { k, v ->
                if (v instanceof String && v.isEmpty()) {
                    model.putAt(k, null)
                }
            }
        }

        validator.validate(model, group).each { ConstraintViolation cv ->
            LOG.info "Adding error path [${cv.propertyPath}] with message [${cv.message}]"
            viewModel.errors[cv.propertyPath.toString()] = cv.message
        }

        !viewModel.hasError()
    }

    def methodMissingHandler = { String name, args ->

        LOG.info "Searching for method [$name] and args [$args]"

        // Check for prefix
        if (prefix=="") {
            LOG.info "No prefix is used for injected methods."
        }  else if (!name.startsWith(prefix)) {
            LOG.error "Missing method $name !"
            throw new MissingMethodException(name, delegate.class, (Object[]) args)
        }

        // Remove prefix
        StringBuffer fName = new StringBuffer()
        fName << name[Math.max(0, prefix.length())].toLowerCase()
        fName << name.substring(Math.max(1, prefix.length()+1))
        String nameWithoutPrefix = fName.toString()
        LOG.info "Method name without prefix [$nameWithoutPrefix]"

        // Checking for injected methods
        switch(nameWithoutPrefix) {

            // findModelByIdNotSoftDeleted
            case ~PATTERN_FINDMODELBYID_NOTSOFTDELETED:
                def match = nameWithoutPrefix =~ PATTERN_FINDMODELBYID_NOTSOFTDELETED
                def modelName = match[0][1]
                LOG.info "First match for model [$modelName] not soft deleted"

                Closure findModelByIdClosure = findModelById(modelName, true)
                delegate.metaClass."$name" = findModelByIdClosure
                return findModelByIdClosure.call(args)

            // findModelById
            case ~PATTERN_FINDMODELBYID:
                def match = (nameWithoutPrefix =~ PATTERN_FINDMODELBYID)
                def modelName = match[0][1]
                LOG.info "First match for model [$modelName]"

                Closure findModelByIdClosure = findModelById(modelName, alwaysExcludeSoftDeleted)
                delegate.metaClass."$name" = findModelByIdClosure
                return findModelByIdClosure.call(args)

            // findModelByDsl
            case ~PATTERN_FINDMODELBYDSL:
                def match = (nameWithoutPrefix =~ PATTERN_FINDMODELBYDSL)
                def isReturnAll = match[0][1]!=null? true: false
                def modelName = match[0][2]
                LOG.info "First match for model [$modelName]"
                Class modelClass = Class.forName(domainClassPackage + "." + modelName)
                delegate.metaClass."$name" = { Object[] p -> findModelByDsl(modelClass, isReturnAll, *p) }
                return findModelByDsl(modelClass, isReturnAll, *args)

            // findModelBy
            case ~PATTERN_FINDMODELBY:
                def match = (nameWithoutPrefix =~ PATTERN_FINDMODELBY)
                def isReturnAll = match[0][1]!=null? true: false
                def modelName = match[0][2]
                def isAnd = (match[0][3] == 'And')
                LOG.info "First match for model [$modelName]"
                Class modelClass = Class.forName(domainClassPackage + "." + modelName)
                delegate.metaClass."$name" = { Object[] p -> findModelBy(modelClass, isReturnAll, isAnd, *p) }
                return findModelBy.call(modelClass, isReturnAll, isAnd, *args)

            // findModelByAttribute
            case ~PATTERN_FINDMODELBYATTRIBUTE:
                def match = (nameWithoutPrefix =~ PATTERN_FINDMODELBYATTRIBUTE)
                def isReturnAll = match[0][1]!=null? true: false
                def modelName = match[0][2]
                def whereExprs = parseFinder(match[0][3])
                LOG.info "First match for model [$modelName]"
                Class modelClass = Class.forName(domainClassPackage + "." + modelName)
                delegate.metaClass."$name" = { Object[] p -> findModelByAttribute(modelClass, isReturnAll, whereExprs, p) }
                return findModelByAttribute.call(modelClass, isReturnAll, whereExprs, args)

            // doNamedQueryOnModel
            case ~PATTERN_DONAMEDQUERY:
                def match = (nameWithoutPrefix =~ PATTERN_DONAMEDQUERY)
                def namedQuery = match[0][1]
                def modelName = match[0][2]
                LOG.info "First match for named query [$namedQuery] on model [$modelName]"
                Closure doNamedQueryClosure = doNamedQuery(namedQuery, modelName)
                delegate.metaClass."$name" = doNamedQueryClosure
                return doNamedQueryClosure.call(args)

            // softDeleteModel
            case ~PATTERN_SOFTDELETE:
                def match = (nameWithoutPrefix =~ PATTERN_SOFTDELETE)
                def modelName = match[0][1]
                LOG.info "First match for model [$model]"
                Closure softDeleteClosure = softDeleteModel(modelName)
                delegate.metaClass."$name" = softDeleteClosure
                return softDeleteClosure.call(args)

            // findAllModel
            case ~PATTERN_FINDALLMODEL:
                def match = nameWithoutPrefix =~ PATTERN_FINDALLMODEL
                def modelName = match[0][1]
                LOG.info "First match for model [$modelName]"

                Closure findAllModelClosure = findAllModel(modelName)
                delegate.metaClass."$name" = findAllModelClosure
                return findAllModelClosure.call(args)


            // Nothing found
            default:
                // Is this one of EntityManager's methods?
                Method method = EntityManager.methods.find { it.name == nameWithoutPrefix }
                if (method) {
                    LOG.info "Executing EntityManager.${nameWithoutPrefix}"
                    executeInsideTransaction {
                        EntityManager em = getEntityManager()
                        return method.invoke(em, args)
                    }
                } else {
                    // No, this is unknown name
                    LOG.error "Missing method $name !"
                    throw new MissingMethodException(name, delegate.class, (Object[])args)
                }
        }
    }
}