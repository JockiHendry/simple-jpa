package simplejpa.scaffolding

class Annotation {

    String name
    Map members = [:]

    public Annotation(String name) {
        this.name = name
    }

    public void addMember(String name, def value) {
        members[name] = value
    }

    public def getMember(String name) {
        members[name]
    }
}
