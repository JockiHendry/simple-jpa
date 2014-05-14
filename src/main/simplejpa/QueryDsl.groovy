package simplejpa

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.Parameter
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class QueryDsl {

    private static Logger LOG = LoggerFactory.getLogger(QueryDsl)
    CriteriaBuilder cb
    Root rootModel
    Predicate criteria
    private String lastJoin
    Map parameters = [:]
    NameConverter nameConverter

    public Predicate getCriteria() {
        if (criteria == null) {
            return cb.conjunction()
        }
        criteria
    }

    def methodMissing(String methodName, methodMissingArgs) {
        String operation = methodMissingArgs['operation'][0]
        def arguments = methodMissingArgs['args'][0]
        LOG.debug "Creating predicate method for attribute $methodName, operation $operation and arguments $arguments..."
        Path attribute = nameConverter.toPath(rootModel, methodName)
        Predicate predicate = getPredicate(operation, attribute, arguments)

        if (criteria==null) {
            criteria = cb.and(cb.conjunction(), predicate)
        } else {
            criteria = cb."$lastJoin"(criteria, predicate)
        }
    }

    private Predicate getPredicate(String operation, Path attribute, def arguments) {
        List paramArgs = []

        // Process arguments
        if (arguments!=null) {
            if (arguments.class.isArray() || arguments instanceof List) {
                if (operation != 'in') {
                    arguments.each {
                        Parameter p = cb.parameter(attribute.javaType)
                        parameters[p] = it
                        paramArgs << p
                    }
                }
            } else {
                Parameter p = cb.parameter(attribute.javaType)
                parameters[p] = arguments
                paramArgs << p
            }
        }

        // Create operations
        if (arguments == null) {
            return cb."$operation"(attribute)
        } else {
            if (operation == 'in') {
                return attribute.in(arguments)
            } else {
                return cb."$operation"(attribute, *paramArgs)
            }
        }
        throw new UnsupportedOperationException("Not supported: $operation, $attribute, $arguments")
    }

    def or() {
        LOG.debug "Conjuction method found..."
        lastJoin = "or"
    }

    def and() {
        LOG.debug "Disjunction method found..."
        lastJoin = "and"
    }

    public static OPERATORS = [
        'greaterThanEqualTo': [operation: "greaterThanOrEqualTo", argsCount: 1],
        'lessThanEqualTo': [operation: "lessThanOrEqualTo", argsCount: 1],
        'greaterThan': [operation: 'greaterThan', argsCount: 1],
        'isNotMember': [operation: 'isNotMember', argsCount: 0],
        'isNotEmpty': [operation: 'isNotEmpty', argsCount: 0],
        'isNotNull': [operation: 'isNotNull', argsCount: 0],
        'notEqual': [operation: 'equal', argsCount: 1],
        'lessThan': [operation: "lessThan", argsCount: 1],
        'isMember': [operation: 'isMember', argsCount: 0],
        'notLike': [operation: 'notLike', argsCount: 1],
        'between': [operation: 'between', argsCount: 2],
        'isEmpty': [operation: 'isEmpty', argsCount: 0],
        'isNull': [operation: 'isNull', argsCount: 0],
        'equal': [operation: 'equal', argsCount: 1],
        'like': [operation: 'like', argsCount: 1],
        'lt': [operation: "lessThan", argsCount: 1],
        'le': [operation: "lessThanOrEqualTo", argsCount: 1],
        'ge': [operation: "greaterThanOrEqualTo", argsCount: 1],
        'gt': [operation: 'greaterThan', argsCount: 1],
        'ne': [operation: 'notEqual', argsCount: 1],
        'eq': [operation: 'equal', argsCount: 1],
        //'isIn': [operation: 'in', argsCount: 1]
    ]

    def eq(arg) {
        [operation: "equal", args: arg]
    }

    def equal(arg) {
        [operation: "equal", args: arg]
    }

    def ne(arg) {
        [operation:  "notEqual", args:  arg]
    }

    def notEqual(arg) {
        [operation:  "notEqual", args:  arg]
    }

    def gt(arg) {
        [operation: "greaterThan", args: arg]
    }

    def greaterThan(arg) {
        [operation: "greaterThan", args:  arg]
    }

    def ge(arg) {
        [operation: "greaterThanOrEqualTo", args:  arg]
    }

    def greaterThanOrEqualTo(arg) {
        [operation: "greaterThanOrEqualTo", args:  arg]
    }

    def lt(arg) {
        [operation: "lessThan", args:  arg]
    }

    def lessThan(arg) {
        [operation: "lessThan", args: arg]
    }

    def le(arg) {
        [operation: "lessThanOrEqualTo", args: arg]
    }

    def between(arg1, arg2) {
        [operation: "between", args:  [arg1, arg2]]
    }

    def isNull() {
        [operation: "isNull", args: null]
    }

    def isNotNull() {
        [operation: "isNotNull", args: null]
    }

    def isEmpty() {
        [operation: "isEmpty", args: null]
    }

    def isNotEmpty() {
        [operation: "isNotEmpty", args: null]
    }

    def isMember(arg) {
        [operation: "isMember", args:  arg]
    }

    def isNotMember(arg) {
        [operation: "isNotMember", args:  arg]
    }

    def like(arg) {
        [operation: "like", args: arg]
    }

    def notLike(arg) {
        [operation: "notLike", args: arg]
    }

    def isIn(arg) {
        [operation: "in", args: arg]
    }

}
