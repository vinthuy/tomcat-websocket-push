package ws.util;

import com.tmall.doom.client.config.ConfigManager;
import ws.model.ServerEndpoint;
import org.apache.commons.lang.StringUtils;

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
                        LocalServerEndpoint.setServerIp(ConfigManager.getCurrentHost());
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

}
