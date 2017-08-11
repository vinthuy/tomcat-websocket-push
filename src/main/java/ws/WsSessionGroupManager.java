package ws;

/**
 * 用于标识websoket之间通信组的group
 * groupKey 标志
 * Created by ruiyong.hry on 09/08/2017.
 */
public interface WsSessionGroupManager {

    //判断一个连接session是否合法
    ServerConnValidDO validAndHandleGroupKey(String groupKey, String clientHost);

}
