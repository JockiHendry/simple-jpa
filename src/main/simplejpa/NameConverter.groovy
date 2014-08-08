package simplejpa

import javax.persistence.criteria.Path
import javax.persistence.criteria.Root
import griffon.util.*

class NameConverter {

    private static String PROPERTY_SEPARATOR = '__'

    public Path toPath(Root root, String name) {
        Path result = null
        if (name.contains(PROPERTY_SEPARATOR)) {
            name.split(PROPERTY_SEPARATOR).each { String node ->
                node = GriffonNameUtils.uncapitalize(node)
                if (!result) {
                    result = root.get(node)
                } else {
                    result = result.get(node)
                }
            }
        } else {
            result = root.get(name)
        }
        result
    }

}
