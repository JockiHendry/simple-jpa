package simplejpa.scaffolding.attribute

import simplejpa.scaffolding.Annotation
import simplejpa.scaffolding.DomainClass

import javax.persistence.FetchType

class CollectionAttribute extends Attribute {

    private static final String[] ANNOTATIONS = ['OneToMany', 'ManyToMany', 'ElementCollection']

    DomainClass target
    String targetType
    String mappedBy
    boolean bidirectional
    boolean manyToMany
    boolean oneToMany
    boolean hasCascadeAndOrphanRemoval
    boolean eager

    public CollectionAttribute(Map information) {
        super(information.'name', information.'type')
        targetType = information.'typeArgument'
        Annotation annotation = information['annotations']?.find { ANNOTATIONS.contains(it.name) }
        if (annotation.name == 'OneToMany' || annotation.name == 'ElementCollection') {
            oneToMany = true
        } else if (annotation.name == 'ManyToMany') {
            manyToMany = true
        }
        mappedBy = annotation.getMember('mappedBy')
        if (mappedBy!=null) bidirectional = true
        hasCascadeAndOrphanRemoval = annotation.getMember("cascade")
        eager = (annotation.getMember("fetch") == FetchType.EAGER)
    }

    public static boolean isInstanceOf(Map information) {
        if (information['type'] == 'List' || information['type'] == 'Set') {
            return information['annotations']?.find { ANNOTATIONS.contains(it.name) }
        }
        false
    }

}
