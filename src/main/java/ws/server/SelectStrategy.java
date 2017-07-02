package ws.server;

/**
 * 选择通道策略,做到客户端均衡负载
 * Created by ruiyong.hry on 02/07/2017.
 */
public interface SelectStrategy{
    int select(int size);
}
