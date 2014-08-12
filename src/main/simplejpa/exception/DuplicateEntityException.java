package simplejpa.exception;

public class DuplicateEntityException extends RuntimeException {

    private Object source;

    public DuplicateEntityException(Object object) {
        super("Duplicate entity: [" + object + "]");
        this.source = object;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

}
