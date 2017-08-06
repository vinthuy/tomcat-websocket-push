package ws.protocol;

/**
 * Created by ruiyong.hry on 04/08/2017.
 */
public interface SenderApi<RS>  {

    public RS SendWsResultProtocol(String clienKey, RS rs, boolean sync);

}
