package simplejpa.ast

import org.codehaus.groovy.ast.MethodNode
import griffon.util.*
import griffon.core.*

class AstUtils {

    public static boolean isValidMethod(GriffonClassUtils.MethodDescriptor method) {
        return GriffonClassUtils.isInstanceMethod(method) &&
            !GriffonClassUtils.isBasicMethod(method) &&
            !GriffonClassUtils.isGroovyInjectedMethod(method) &&
            !GriffonClassUtils.isThreadingMethod(method) &&
            !GriffonClassUtils.isArtifactMethod(method) &&
            !GriffonClassUtils.isMvcMethod(method) &&
            !GriffonClassUtils.isServiceMethod(method) &&
            !GriffonClassUtils.isEventPublisherMethod(method) &&
            !GriffonClassUtils.isObservableMethod(method) &&
            !GriffonClassUtils.isResourceHandlerMethod(method) &&
            !GriffonClassUtils.isContributionMethod(method);
    }

    public static boolean isValidMethod(MethodNode methodNode) {
        isValidMethod(methodDescriptorFor(methodNode))
    }

    public static GriffonClassUtils.MethodDescriptor methodDescriptorFor(MethodNode method) {
        if (method==null) return null
        String[] parameterTypes = method.getParameters().collect { it.getType().getPlainNodeReference().getName() }
        new GriffonClassUtils.MethodDescriptor(method.name, parameterTypes, method.modifiers)
    }


}
