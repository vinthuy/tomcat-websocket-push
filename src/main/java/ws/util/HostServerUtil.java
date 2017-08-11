package ws.util;

import ws.model.ServerEndpoint;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by ruiyong.hry on 14/07/2017.
 */
public class HostServerUtil {

    private static ServerEndpoint LocalServerEndpoint = new ServerEndpoint();


    public static String getLocalIp() {
        try {
            if (StringUtils.isBlank(LocalServerEndpoint.getServerIp())) {
                synchronized (HostServerUtil.class) {
                    if (StringUtils.isBlank(LocalServerEndpoint.getServerIp())) {
                        String host = InetAddress.getLocalHost().getHostAddress();
                        LocalServerEndpoint.setServerIp(host);
                    }
                }
            }
        } catch (UnknownHostException e) {
            //ignore
        }
        return LocalServerEndpoint.getServerIp();
    }

    public static int getPort() {
        return LocalServerEndpoint.getPort();
    }

    public static void setPort(int port) {
        LocalServerEndpoint.setPort(port);
    }

    @Setter
    @Getter
    public static int currentServerEnv; //当前服务器环境标识


}
