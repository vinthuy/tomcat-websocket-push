package ws.protocol;

/**
 * Э�鴦��
 * ����,�ͷ���ֵ
 * Created by ruiyong.hry on 04/08/2017.
 */
public interface WsTsPortHandle<RE, RS> {

    RS handle(RE request);

}
