package ws.protocol;

/**
 * 客户端协议处理默认实现
 * 请求,和返回值
 * Created by ruiyong.hry on 04/08/2017.
 */
public interface WsTsPortHandle<RE, RS> {

    RS handle(RE request);

}
