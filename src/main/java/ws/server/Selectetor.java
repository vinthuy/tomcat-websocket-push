package ws.server;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * ѡ��ͨ������,�����ͻ��˾��⸺��
 * Created by ruiyong.hry on 02/07/2017.
 */
public interface Selectetor {

    //һ�������ڵ�������ʽ
    PushToClientSession select(ConcurrentSkipListMap<String, PushToClientSession> skipListMap);

}
