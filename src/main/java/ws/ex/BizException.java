package ws.ex;

/**
 * Created by ruiyong.hry on 05/06/2017.
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }
}
