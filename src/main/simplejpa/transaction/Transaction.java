package simplejpa.transaction;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@GroovyASTTransformationClass("simplejpa.transaction.TransactionTransformation")
public @interface Transaction {

    Policy value() default Policy.NORMAL;

    boolean newSession() default false;

    public enum Policy { NORMAL, SKIP_PROPAGATION, SKIP }

}
