package ws.protocol;

/**
 * �ͻ���Э�鴦��Ĭ��ʵ��
 * ����,�ͷ���ֵ
 * Created by ruiyong.hry on 04/08/2017.
 */
public interface WsTsPortHandle<RE, RS> {

    RS handle(RE request);

}
