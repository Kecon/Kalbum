package se.kecon.kalbum.validation;

/**
 * IllegalPasswordException is thrown when a password is not valid.
 *
 * @author Kenny Colliander
 * @since 2023-08-11
 */
public class IllegalPasswordException extends Exception {
    public IllegalPasswordException() {
        super();
    }

    public IllegalPasswordException(String message) {
        super(message);
    }

    public IllegalPasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalPasswordException(Throwable cause) {
        super(cause);
    }
}
