package simplejpa.scaffolding.attribute

import simplejpa.scaffolding.Annotation
import simplejpa.scaffolding.DomainClass

class EntityAttribute extends Attribute {

    private static final String[] ANNOTATIONS = ['ManyToOne', 'OneToOne']

    DomainClass target
    boolean manyToOne
    boolean oneToOne
    String mappedBy
    boolean hasCascadeAndOrphanRemoval

    public EntityAttribute(Map information) {
        super(information.'name', information.'type')
        Annotation annotation = information['annotations']?.find { ANNOTATIONS.contains(it.name) }
        if (annotation.name == 'ManyToOne') {
            manyToOne = true
        } else if (annotation.name == 'OneToOne') {
            oneToOne = true
            mappedBy = annotation.getMember('mappedBy')
            hasCascadeAndOrphanRemoval = annotation.getMember("cascade")
        }
    }

    public boolean isInverse() {
        if (manyToOne) {
            return target.attributes.find { it instanceof CollectionAttribute && it.oneToMany &&
                it.hasCascadeAndOrphanRemoval && it.mappedBy}
        } else if (oneToOne) {
            return mappedBy && !hasCascadeAndOrphanRemoval
        }
        false
    }

    public static boolean isInstanceOf(Map information) {
        information['annotations']?.find { ANNOTATIONS.contains(it.name) }
    }

}
