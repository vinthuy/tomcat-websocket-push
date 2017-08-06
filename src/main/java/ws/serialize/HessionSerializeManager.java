package ws.serialize;

import com.tmall.doom.client.util.HessianUtils;

/**
 * Created by ruiyong.hry on 03/08/2017.
 */
public class HessionSerializeManager implements SerializeManager {
    @Override
    public byte[] serialize(Object obj) throws Exception {
        return HessianUtils.serialize(obj);
    }

    @Override
    public Object deserialize(byte[] bytes) throws Exception {
        return HessianUtils.deserialize(bytes);
    }
}
