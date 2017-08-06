package ws.server;

/**
 * the message forwarder.
 * Created by ruiyong.hry on 25/07/2017.
 */
public interface MsgForwarder {

    /**
     * 存放大对象,返回对象key
     *
     * @param txt
     * @return
     */
    public String putMsg(String txt);


    /**
     * 根据key得到数据
     *
     * @param key
     * @return
     */
    public String getMsg(String key);

    /**
     * 清除数据
     *
     * @param key
     * @return
     */
    public void clearMsg(String key);
}
