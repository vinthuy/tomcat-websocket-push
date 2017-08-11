package ws;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsConstants {
    public static final Logger wslogger = LoggerFactory.getLogger(WsConstants.class);

    public static final String requestHeart = "heart0";
    public static final String responsetHeart = "heart1";

    public static final byte[] requestHeartBytes = requestHeart.getBytes();
    public static final byte[] reponseHeartBytes = responsetHeart.getBytes();
//    public static final ByteBuffer heartBuffer = ByteBuffer.wrap(requestHeart.getBytes());


    public static final String api_error = "-501";
    public static final String api_param_error = "-500";

    //��Ϣ���
    public static final byte msg_heart = -11;

    public static final String clientHost = "ch";

    /**
     * ���͵���Ϣ��������[������Ϣ]
     */
    public final static byte MSG_TYPE_HANDLE_RETURN = 1;

    /**
     * ���ص���Ϣ����[��ȥ��Ϣ]
     */
    public final static byte MSG_TYPE_SENDONLY = 2;
}
