package ws.server;

/**
 * ������Ϣѡ��groupKey
 * Created by ruiyong.hry on 11/08/2017.
 */
public interface GroupKeySelector {

    public String selectByMessage(Object message);

}
