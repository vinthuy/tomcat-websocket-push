package ws.protocol;

/**
 * 协议处理
 * 请求,和返回值
 * Created by ruiyong.hry on 04/08/2017.
 */
public interface WsTsPortHandle<RE, RS> {

    RS handle(RE request);

}
