package simplejpa

import org.codehaus.groovy.transform.GroovyASTTransformationClass
import javax.persistence.GenerationType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass("simplejpa.DomainClassTransformation")
public @interface DomainClass {
    boolean excludeId() default false
    boolean excludeAuditing() default false
    boolean excludeDeletedFlag() default false
    GenerationType idGenerationStrategy() default GenerationType.TABLE
}
