package ws;

/**
 * Created by ruiyong.hry on 09/08/2017.
 */
public class WsException extends RuntimeException {

    public WsException(String message) {
        super(message);
    }

    public WsException(String message, Throwable cause) {
        super(message, cause);
    }

}
