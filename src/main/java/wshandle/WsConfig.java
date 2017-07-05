package wshandle;


import ws.PushMsgType;

/**
 * the ws-config
 * Created by ruiyong.hry on 04/07/2017.
 */
public class WsConfig {

    public enum PushServerKey {
        ALI_DAILY("ali-daily"), ALI_PRE("ali-pre"),ALI_US_PRE("ali-uspre");

        PushServerKey(String key) {
            this.key = key;
        }

        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public static PushServerKey get(String key) {
            for (PushServerKey pushServerKey : PushServerKey.values()) {
                if (pushServerKey.key.equalsIgnoreCase(key)) {
                    return pushServerKey;
                }
            }
            return null;
        }
    }


}
