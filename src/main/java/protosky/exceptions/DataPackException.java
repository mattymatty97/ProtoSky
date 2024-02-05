package protosky.exceptions;

public class DataPackException extends RuntimeException{
    public DataPackException() {
    }

    public DataPackException(String message) {
        super(message);
    }

    public DataPackException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataPackException(Throwable cause) {
        super(cause);
    }

    public DataPackException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
