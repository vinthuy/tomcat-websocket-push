package ws.model;

import lombok.Data;

/**
 * Created by ruiyong.hry on 25/07/2017.
 */
@Data
public class PullMsg {
    private String key;//标记环境的key
    private String msgKey;//标记消息key

}
