package ws.model;

import com.google.common.base.Objects;
import lombok.Data;

import java.io.Serializable;

/**
 * the EnvCacheValue
 * Created by ruiyong.hry on 14/07/2017.
 */
@Data
public class EnvCacheValue implements Serializable {
    //key ---
    private ServerEndpoint serverEndpoint;
    private int serverEnv;
    //value
    private int sessionCount;


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("serverEndpointIp", serverEndpoint.getServerIp())
                .add("serverEndpointPort", serverEndpoint.getPort())
                .add("serverEnv", serverEnv)
                .toString();
    }
}
