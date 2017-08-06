package start;


import lombok.Data;
import ws.WsContainer;

/**
 * Created by ruiyong.hry on 04/07/2017.
 */
@Data
public class EnvConfig extends WsContainer.WsConfigDO {

    private String homeZone;

    private int env;

    private boolean connectUsFromDaily = false;

    private String dailyHttpUrl;

    private String preHttpUrl;

    private String onlineHttpUrl;

    private String currentUrl;

    private String usPreHttpUrl;

    private String usOnlineHttpUrl;

    //通过此参数可以加强连接的负载均衡.
    private int clientCount = 2;

}
