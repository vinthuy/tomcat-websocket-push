import com.alibaba.fastjson.JSON;
import org.junit.Test;
import ws.PushMsgType;
import ws.WsContainerSingle;
import ws.model.WsResult;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class WsEvnStartExTest {

    @Test
    public void test() throws Exception {
        WsEvnStartEx wsEvnStartEx = new WsEvnStartEx();
        wsEvnStartEx.afterPropertiesSet();
    }

    public static void main(String[] args) {
        WsEvnStartExTest wsEvnStartExTest = new WsEvnStartExTest();
        try {
            wsEvnStartExTest.test();
        } catch (Exception e) {
            e.printStackTrace();
        }
        WsResult wsResult = new WsResult();
        wsResult.setMsg("hello123");
        wsResult.setFlag(PushMsgType.OK.getTag());
        wsResult.setSuccess(true);
        try {
            wsResult =  WsContainerSingle.instance().getWsProxyClientRadmon().getSessionIfCloseNew().sendObj(wsResult,true);
            System.out.println(JSON.toJSONString(wsResult));
            System.out.println("-----------");
            wsResult =  WsContainerSingle.instance().getWsProxyClientRadmon().getSessionIfCloseNew().sendObj(wsResult,false);
            System.out.println(JSON.toJSONString(wsResult));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
