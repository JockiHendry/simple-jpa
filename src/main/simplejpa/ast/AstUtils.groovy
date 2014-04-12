package simplejpa.ast

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.MethodNode
import griffon.util.*
import griffon.core.*
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.runtime.StringGroovyMethods

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

    public static List<String> getMemberList(AnnotationNode anno, String name) {
        List<String> list;
        Expression expr = anno.getMember(name);
        if (expr != null && expr instanceof ListExpression) {
            list = new ArrayList<String>();
            final ListExpression listExpression = (ListExpression) expr;
            for (Expression itemExpr : listExpression.getExpressions()) {
                if (itemExpr != null && itemExpr instanceof ConstantExpression) {
                    Object value = ((ConstantExpression) itemExpr).getValue();
                    if (value != null) list.add(value.toString());
                }
            }
        } else {
            String rawExcludes = getMemberStringValue(anno, name)
            list = (rawExcludes == null ? new ArrayList<String>() : StringGroovyMethods.tokenize(rawExcludes, ", "))
        }
        return list;
    }

    public static String getMemberStringValue(AnnotationNode node, String name) {
        final Expression member = node.getMember(name);
        if (member != null && member instanceof ConstantExpression) {
            Object result = ((ConstantExpression) member).getValue();
            if (result != null) return result.toString();
        }
        return null;
    }

}
