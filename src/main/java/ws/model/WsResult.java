package ws.model;

import java.io.Serializable;

/**
 * 传输实体
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsResult implements Serializable {

    public boolean success;

    private Object data;

    private String msg;

    //数据类型
    private int flag;


    public static WsResult buildSuccessWsResult(int flag, Object data) {
        WsResult wsResult = new WsResult();
        wsResult.setData(data);
        wsResult.setFlag(flag);
        wsResult.setSuccess(true);
        return wsResult;
    }

    public static WsResult buildFailedWsResult(int flag, Object data) {
        WsResult wsResult = new WsResult();
        wsResult.setData(data);
        wsResult.setFlag(flag);
        wsResult.setSuccess(false);
        return wsResult;
    }

    public static WsResult buildSimpleSuccessResult() {
        WsResult wsResult = new WsResult();
        wsResult.setFlag(0);
        wsResult.setSuccess(true);
        return wsResult;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
