package se.kecon.kalbum.validation;

public class IllegalEmailException extends Exception {
    public IllegalEmailException() {
        super();
    }

    public IllegalEmailException(String message) {
        super(message);
    }

    public IllegalEmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalEmailException(Throwable cause) {
        super(cause);
    }
}
