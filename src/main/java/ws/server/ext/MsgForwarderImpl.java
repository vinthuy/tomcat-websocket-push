package ws.server.ext;

import ws.DistributeCacheService;
import ws.server.MsgForwarder;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * Created by ruiyong.hry on 25/07/2017.
 */
public class MsgForwarderImpl implements MsgForwarder {

    private final static String cache_prefix = "doom-fwd-";

    @Resource
    private DistributeCacheService tairCommonService;

    //保存10分钟就行了
    private int expirdTime = 3 * 60;


    @Override
    public String putMsg(String txt) {
        String uuid = UUID.randomUUID().toString();
        String cacheKey = getCaCheKey(uuid);
        try {
            tairCommonService.put(cacheKey, txt, expirdTime);
        } catch (Exception e) {
            //
        }
        return uuid;
    }

    @Override
    public String getMsg(String msgKey) {
        String cacheKey = getCaCheKey(msgKey);
        String val = null;
        try {
            val = (String) tairCommonService.get(cacheKey);
        } catch (Exception e) {
            //
        }
        return val;
    }

    @Override
    public void clearMsg(String msgKey) {
        String cacheKey = getCaCheKey(msgKey);
        try {
            tairCommonService.remove(cacheKey);
        } catch (Exception e) {
            //
        }
    }

    private String getCaCheKey(String key) {
        return cache_prefix + key;
    }


}
