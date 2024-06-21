package se.kecon.kalbum.validation;

public class IllegalUsernameException extends Exception {

    public IllegalUsernameException() {
        super();
    }

    public IllegalUsernameException(String message) {
        super(message);
    }

    public IllegalUsernameException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalUsernameException(Throwable cause) {
        super(cause);
    }
}
