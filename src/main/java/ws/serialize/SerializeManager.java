package ws.serialize;

/**
 * 序列化管理器
 * Created by ruiyong.hry on 03/08/2017.
 */
public interface SerializeManager {

    byte[] serialize(Object obj) throws Exception;

    public  Object deserialize(byte[] bytes) throws Exception;

}
