package ws;

/**
 * ���ڱ�ʶwebsoket֮��ͨ�����group
 * groupKey ��־
 * Created by ruiyong.hry on 09/08/2017.
 */
public interface WsSessionGroupManager {

    //�ж�һ������session�Ƿ�Ϸ�
    ServerConnValidDO validAndHandleGroupKey(String groupKey, String clientHost);

}
