package ws.server.ext;

import com.google.common.base.Objects;
import lombok.Data;

import java.io.Serializable;

/**
 * the wsClient
 * Created by ruiyong.hry on 14/07/2017.
 */
@Data
public class WsServerInterResult<RS> implements Serializable {

    /**
     * 当前请求成功还是失败
     */
    private boolean success;

    /**
     * 返回的数据
     */
    private RS data;

    private String msg;

    public WsServerInterResult() {
    }

    public WsServerInterResult(boolean success, RS data) {
        this.success = success;
        this.data = data;
    }

    public WsServerInterResult(boolean success, RS data, String msg) {
        this.success = success;
        this.data = data;
        this.msg = msg;
    }


    public static WsServerInterResult buildFailureResult(String failReason) {
        return new WsServerInterResult(false, failReason);
    }

    public static WsServerInterResult buildSuccessResult(Object data) {
        return new WsServerInterResult(true, data);
    }

    public static WsServerInterResult buildFailureResultAtMsg(String failReason) {
        return new WsServerInterResult(false, null, failReason);
    }

    public static WsServerInterResult buildSuccessResultAtMsg(Object data, String msg) {
        return new WsServerInterResult(true, data, msg);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("success", success)
                .add("data", data)
                .add("msg", msg)
                .toString();
    }
}
