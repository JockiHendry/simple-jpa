package simplejpa.exception;

public class EntityNotFoundException extends RuntimeException {

    private Object source;

    public EntityNotFoundException(Object object) {
        super("Entity Not Found: [" + object + "]");
        this.source = object;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

}
