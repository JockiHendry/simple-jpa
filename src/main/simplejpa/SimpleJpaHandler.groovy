package simplejpa

import org.codehaus.groovy.runtime.metaclass.ConcurrentReaderHashMap
import org.slf4j.*
import simplejpa.transaction.EntityManagerLifespan
import simplejpa.transaction.ReturnFailedSignal
import simplejpa.transaction.TransactionHolder

import javax.persistence.*
import griffon.util.*
import griffon.core.GriffonApplication

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
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
    private static final PATTERN_FINDMODELBYDSL = /find([A-Z]\w*)ByDsl/
    private static final PATTERN_FINDMODELBYATTRIBUTE = /find([A-Z]\w*)By([A-Z]\w*)/
    private static final PATTERN_FINDMODELBY = /find([A-Z]\w*)By/
    private static final PATTERN_DONAMEDQUERY = /do([A-Z]\w*)On([A-Z]\w*)/
    private static final PATTERN_SOFTDELETE = /softDelete([A-Z]\w*)/

    private static final int DEFAULT_PAGE_SIZE = 10

    final String prefix
    final String domainModelPackage
    final EntityManagerFactory emf
    final Validator validator
    final boolean alwaysExcludeSoftDeleted
    final EntityManagerLifespan entityManagerLifespan

    private boolean convertEmptyStringToNull
    private FlushModeType defaultFlushMode

    public SimpleJpaHandler(EntityManagerFactory emf, Validator validator, String prefix, String domainModelPackage,
            boolean alwaysExcludeSoftDeleted, EntityManagerLifespan entityManagerLifespan) {
        this.emf = emf
        this.validator = validator
        this.prefix = (prefix==null || prefix=="" || prefix=="[]") ? "" : prefix
        this.domainModelPackage = domainModelPackage
        this.alwaysExcludeSoftDeleted = alwaysExcludeSoftDeleted
        this.entityManagerLifespan = entityManagerLifespan

        // Check configurations
        GriffonApplication app = ApplicationHolder.application
        convertEmptyStringToNull = app.config.griffon?.simplejpa?.validation.convertEmptyStringToNull ?: false
    }

    final ConcurrentReaderHashMap mapTransactionHolder = new ConcurrentReaderHashMap()

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
            def flushModeConfig = ApplicationHolder.application.config.griffon?.simplejpa?.entityManager.defaultFlushMode
            if (flushModeConfig) {
                if (flushModeConfig instanceof String) {
                    defaultFlushMode = FlushModeType.valueOf(flushModeConfig)
                } else {
                    defaultFlushMode = flushModeConfig
                }
                em.setFlushMode(defaultFlushMode)
                LOG.info "Set flushmode to $defaultFlushMode"
            }
            th = new TransactionHolder(em)
        }
        mapTransactionHolder.put(Thread.currentThread(), th)
        debugEntityManager()
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
                    Class subclass = Class.forName("${domainModelPackage}.${it.trim()}")
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
        th.beginTransaction(resume)
    }

    def closeAndRemoveCurrentEntityManager = {
        TransactionHolder th = mapTransactionHolder.get(Thread.currentThread())
        if (th) {
            th.em.close()
            LOG.info "EntityManager for $th is closed!"
            mapTransactionHolder.remove(Thread.currentThread())
            LOG.info "EntityManager for thread ${Thread.currentThread()} is removed!"
        }
    }

    def commitTransaction = {
        LOG.info "Commit transaction from thread ${Thread.currentThread()} (${Thread.currentThread().id})..."
        mapTransactionHolder.get(Thread.currentThread())?.commitTransaction()
        if (entityManagerLifespan==EntityManagerLifespan.TRANSACTION) {
            closeAndRemoveCurrentEntityManager()
        }
    }

    def rollbackTransaction = {
        LOG.info "Rollback transaction from thread ${Thread.currentThread()} (${Thread.currentThread().id})..."
        TransactionHolder th = mapTransactionHolder.get(Thread.currentThread())
        th.em.clear()
        th.rollbackTransaction()
        closeAndRemoveCurrentEntityManager()
        if (entityManagerLifespan==EntityManagerLifespan.MANUAL) {
            createEntityManager(th)
        }
    }

    def executeInsideTransaction(Closure action) {
        boolean insideTransaction = true
        boolean isError = false
        def result
        if (!getEntityManager()?.transaction?.isActive()) {
            insideTransaction = false
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
                Root rootModel = c.from(Class.forName(domainModelPackage + "." + model))
                c.select(rootModel)

                configureCriteria(cb, c, rootModel, config)
                configureQuery(getEntityManager().createQuery(c), config).getResultList()
            }
        }
    }

    def findModelById = { String model, boolean notSoftDeleted ->
        def modelClass = Class.forName(domainModelPackage + "." + model)

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

    def findModelByDsl = { String model ->
        Class modelClass = Class.forName(domainModelPackage + "." + model)

        return { Map config = [:], Closure closure ->
            LOG.info "Executing find${model}ByDsl with config [$config]"
            executeInsideTransaction {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder()
                CriteriaQuery c = cb.createQuery()
                Root rootModel = c.from(modelClass)
                c.select(rootModel)

                closure.delegate = new QueryDsl(cb: cb, rootModel: rootModel)
                closure.call()
                c.where(closure.delegate.criteria)

                configureCriteria(cb, c, rootModel, config)
                configureQuery(getEntityManager().createQuery(c), config).getResultList()
            }
        }
    }

    def findModelBy = { String model ->
        Class modelClass = Class.forName(domainModelPackage + "." + model)

        return { Map args, Map config = [:]  ->
            LOG.info "Executing find${model}By with argument [$args] and config [$config]"
            executeInsideTransaction {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder()
                CriteriaQuery c = cb.createQuery()
                Root rootModel = c.from(modelClass)
                c.select(rootModel)

                Predicate criteria
                criteria = cb.conjunction()
                args.each { key, value ->
                    criteria = cb.and(criteria, cb.equal(rootModel.get(key), value))
                }

                c.where(criteria)

                configureCriteria(cb, c, rootModel, config)
                configureQuery(getEntityManager().createQuery(c), config).getResultList()
            }
        }
    }

    def findModelByAttribute = { String model, String attribute ->
        Class modelClass = Class.forName(domainModelPackage + "." + model)

        return { Object[] args ->
            LOG.info "Executing find${model}By${attribute} with argument [$args]"
            executeInsideTransaction {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder()
                CriteriaQuery c = cb.createQuery()
                Root rootModel = c.from(modelClass)
                c.select(rootModel)

                if (args.length > 1 && args[0] instanceof String && !(args[1] instanceof Map) ) {
                    LOG.info "Operation [${args[0]}]..."
                    def lastArgumentIndex = (args.last() instanceof Map) ? args.length - 2 : args.length - 1
                    c.where(cb."${GriffonNameUtils.uncapitalize(args[0])}"(rootModel.get(attribute), *args[1..lastArgumentIndex]))
                } else {
                    LOG.info "Operation [eq]..."
                    c.where(cb.equal(rootModel.get(attribute), args[0]))
                }

                if (args.last() instanceof Map) {
                    Map configuration = (Map) args.last()
                    configureCriteria(cb, c, rootModel, configuration)
                    return configureQuery(getEntityManager().createQuery(c), configuration).getResultList()
                } else {
                    return getEntityManager().createQuery(c).getResultList()
                }
            }
        }
    }

    def doNamedQuery = { String namedQuery, String model ->
        Query query = getEntityManager().createNamedQuery("${model}.${namedQuery}")

        return { Map args, Map config = [:] ->
            LOG.info "Executing named query [${model}.${namedQuery}] with argument [$args]"
            executeInsideTransaction {
                args.each { key, value ->
                    query.setParameter(key, value)
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

    def validate = { viewModel ->
        return { Object[] args ->
            def model = args[0]
            def group = Default
            if (args.length > 1) {
                group = args[1]
            }
            LOG.info "Validating model [$model] group [$group]"

            // Make sure no existing errors before validating
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

            return !viewModel.hasError()
        }
    }

    def methodMissingHandler = { String name, args ->

        LOG.info "Searching for method [$name] and args [$args]"

        // Transaction method's name without prefix should always available
        switch (name) {
            case "beginTransaction":
                delegate.metaClass.beginTransaction = beginTransaction
                return beginTransaction(*args)
            case "commitTransaction":
                delegate.metaClass.commitTransaction = commitTransaction
                return commitTransaction()
            case "rollbackTransaction":
                delegate.metaClass.rollbackTransaction = rollbackTransaction
                return rollbackTransaction()
            case "return_failed":
                delegate.metaClass.return_failed = returnFailed
                return returnFailed()
            case "createEntityManager":
                delegate.metaClass.createEntityManager = createEntityManager
                return createEntityManager(*args)
            case "destroyEntityManager":
                delegate.metaClass.destroyEntityManager = destroyEntityManager
                return destroyEntityManager()
            case "getEntityManager":
                delegate.metaClass.getEntityManager = getEntityManager
                return getEntityManager()
        }

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

            case "beginTransaction":
                delegate.metaClass."$name" = beginTransaction
                return beginTransaction(*args)

            case "commitTransaction":
                delegate.metaClass."$name" = commitTransaction
                return commitTransaction()

            case "rollbackTransaction":
                delegate.metaClass."$name" = rollbackTransaction
                return rollbackTransaction()

            case "persist":
                delegate.metaClass.persist = persist
                return persist(args[0])

            case "merge":
                delegate.metaClass.merge = merge
                return merge(args[0])

            case "remove":
                delegate.metaClass.remove = remove
                return remove(args[0])

            case "softDelete":
                delegate.metaClass.softDelete = softDelete
                return softDelete(args[0])

            case "getEntityManager":
                delegate.metaClass.getEntityManager = getEntityManager
                return getEntityManager()

            case "executeQuery":
                delegate.metaClass.executeQuery = executeQuery
                return executeQuery(args[0])

            case "executeNativeQuery":
                delegate.metaClass.executeNativeQuery = executeNativeQuery
                return executeNativeQuery(args[0])

            case "validate":
                Closure validateClosure = validate(delegate.model)
                delegate.metaClass.validate = validateClosure
                return validateClosure.call(args)

            // findAllModel
            case ~PATTERN_FINDALLMODEL:
                def match = nameWithoutPrefix =~ PATTERN_FINDALLMODEL
                def modelName = match[0][1]
                LOG.info "First match for model [$modelName]"

                Closure findAllModelClosure = findAllModel(modelName)
                delegate.metaClass."$name" = findAllModelClosure
                return findAllModelClosure.call(args)

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
                def modelName = (nameWithoutPrefix =~ PATTERN_FINDMODELBYDSL)[0][1]
                LOG.info "First match for model [$modelName]"
                Closure findModelByDslClosure = findModelByDsl(modelName)
                delegate.metaClass."$name" = findModelByDslClosure
                return findModelByDslClosure.call(args)

            // findModelBy
            case ~PATTERN_FINDMODELBY:
                def modelName = (nameWithoutPrefix =~ PATTERN_FINDMODELBY)[0][1]
                LOG.info "First match for model [$modelName]"
                Closure findModelByClosure = findModelBy(modelName)
                delegate.metaClass."$name" = findModelByClosure
                return findModelByClosure.call(*args)

            // findModelByAttribute
            case ~PATTERN_FINDMODELBYATTRIBUTE:
                def match = (nameWithoutPrefix =~ PATTERN_FINDMODELBYATTRIBUTE)
                def modelName = match[0][1]
                def attributeName = GriffonNameUtils.uncapitalize(match[0][2])
                LOG.info "First match for model [$modelName] attribute [$attributeName]"
                Closure findModelByAttributeClosure = findModelByAttribute(modelName, attributeName)
                delegate.metaClass."$name" = findModelByAttributeClosure
                return findModelByAttributeClosure.call(args)

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
