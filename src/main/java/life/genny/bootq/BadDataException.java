package life.genny.bootq;


@SuppressWarnings("serial")
public class BadDataException extends Exception {

    public BadDataException() {
        super();
    }

    public BadDataException(String message) {
        super(message);
    }
}