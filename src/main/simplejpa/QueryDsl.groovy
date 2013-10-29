package simplejpa

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class QueryDsl {

    private static Logger LOG = LoggerFactory.getLogger(QueryDsl)

    CriteriaBuilder cb
    Root rootModel
    Predicate criteria
    private String lastJoin

    def methodMissing(String methodName, args) {
        def operation = args['operation'][0]
        def arguments = args['args'][0]
        LOG.info "Creating predicate method for attribute $methodName, operation $operation and arguments $arguments..."
        Predicate predicate
        if (arguments==null) {
            predicate = cb."$operation"(rootModel.get(methodName))
        } else if (arguments.class.isArray() || arguments instanceof List) {
            predicate = cb."$operation"(rootModel.get(methodName), *arguments)
        } else {
            predicate = cb."$operation"(rootModel.get(methodName), arguments)
        }
        if (criteria==null) {
            criteria = cb.conjunction()
            criteria = cb.and(criteria, predicate)
        } else {
            criteria = cb."$lastJoin"(criteria, predicate)
        }
    }

    def or() {
        LOG.info "Conjuction method found..."
        lastJoin = "or"
    }

    def and() {
        LOG.info "Disjunction method found..."
        lastJoin = "and"
    }

    public static OPERATORS = [
        'eq': [operation: 'equal', argsCount: 1],
        'equal': [operation: 'equal', argsCount: 1],
        'ne': [operation: 'notEqual', argsCount: 1],
        'notEqual': [operation: 'equal', argsCount: 1],
        'gt': [operation: 'greaterThan', argsCount: 1],
        'greaterThan': [operation: 'greaterThan', argsCount: 1],
        'ge': [operation: "greaterThanOrEqualTo", argsCount: 1],
        'greaterThanEqualTo': [operation: "greaterThanOrEqualTo", argsCount: 1],
        'lt': [operation: "LessThan", argsCount: 1],
        'lessThan': [operation: "LessThan", argsCount: 1],
        'le': [operation: "LessThanOrEqualTo", argsCount: 1],
        'lessThanEqualTo': [operation: "LessThanOrEqualTo", argsCount: 1],
        'between': [operation: 'between', argsCount: 2],
        'isNull': [operation: 'isNull', argsCount: 0],
        'isNotNull': [operation: 'isNotNull', argsCount: 0],
        'isEmpty': [operation: 'isEmpty', argsCount: 0],
        'isNotEmpty': [operation: 'isNotEmpty', argsCount: 0],
        'isMember': [operation: 'isMember', argsCount: 0],
        'isNotMember': [operation: 'isNotMember', argsCount: 0],
        'like': [operation: 'like', argsCount: 1],
        'notLike': [operation: 'notLike', argsCount: 1],
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

}
