package ws.server;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 选择通道策略,做到客户端均衡负载
 * Created by ruiyong.hry on 02/07/2017.
 */
public interface Selectetor {

    //一个进程内的索引方式
    PushToClientSession select(ConcurrentSkipListMap<String, PushToClientSession> skipListMap);

}
