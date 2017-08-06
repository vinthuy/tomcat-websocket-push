package ws.model;

import com.google.common.base.Objects;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by ruiyong.hry on 14/07/2017.
 */
public class ServerEndpoint implements Serializable {
    private String serverIp;
    private int port;

    public ServerEndpoint(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public ServerEndpoint() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerEndpoint)) return false;
        ServerEndpoint that = (ServerEndpoint) o;
        return port == that.port &&
                Objects.equal(serverIp, that.serverIp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serverIp, port);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("serverIp", serverIp)
                .add("port", port)
                .toString();
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
