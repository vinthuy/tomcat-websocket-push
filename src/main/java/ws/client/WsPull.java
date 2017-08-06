package ws.client;

import ws.model.PullMsg;

/**
 * Created by ruiyong.hry on 25/07/2017.
 */
public interface WsPull {

    public String pull(PullMsg pullMsg);

}
