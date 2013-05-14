package simplejpa.transaction

import org.codehaus.groovy.transform.GroovyASTTransformationClass
import simplejpa.transaction.SimpleJpaTransaction.Policy

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.TYPE, ElementType.METHOD, ElementType.FIELD])
@GroovyASTTransformationClass("simplejpa.transaction.TransactionTransformation")
public @interface SimpleJpaTransaction {

    Policy value() default Policy.PROPAGATE

    public enum Policy { PROPAGATE, NO_PROPAGATE, SKIP }

}
