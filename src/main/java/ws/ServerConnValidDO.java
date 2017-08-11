package ws;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by ruiyong.hry on 11/08/2017.
 */
@Data
public class ServerConnValidDO implements Serializable {

    protected boolean valid;

    protected String groupKey;


    public ServerConnValidDO(String groupKey, boolean valid) {
        this.valid = valid;
        this.groupKey = groupKey;
    }
}
