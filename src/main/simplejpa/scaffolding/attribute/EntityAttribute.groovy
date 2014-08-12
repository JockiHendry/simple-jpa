package simplejpa.scaffolding.attribute

import simplejpa.scaffolding.Annotation
import simplejpa.scaffolding.DomainClass

class EntityAttribute extends Attribute {

    private static final String[] ANNOTATIONS = ['ManyToOne', 'OneToOne', 'Embedded']

    DomainClass target
    boolean manyToOne
    boolean oneToOne
    boolean embedded
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
        } else if (annotation.name == 'Embedded') {
            embedded = true
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

    public String getActionName() {
        "show${type}"
    }

    public boolean isPair() {
        (oneToOne && !inverse) || embedded
    }

    public static boolean isInstanceOf(Map information) {
        information['annotations']?.find { ANNOTATIONS.contains(it.name) }
    }

}
