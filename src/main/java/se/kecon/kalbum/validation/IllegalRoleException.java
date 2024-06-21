package se.kecon.kalbum.validation;

public class IllegalRoleException extends Exception {

    public IllegalRoleException() {
        super();
    }

    public IllegalRoleException(String message) {
        super(message);
    }

    public IllegalRoleException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalRoleException(Throwable cause) {
        super(cause);
    }
}
