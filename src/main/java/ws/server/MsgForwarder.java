package ws.server;

/**
 * the message forwarder.
 * Created by ruiyong.hry on 25/07/2017.
 */
public interface MsgForwarder {

    /**
     * ��Ŵ����,���ض���key
     *
     * @param txt
     * @return
     */
    public String putMsg(String txt);


    /**
     * ����key�õ�����
     *
     * @param key
     * @return
     */
    public String getMsg(String key);

    /**
     * �������
     *
     * @param key
     * @return
     */
    public void clearMsg(String key);
}
