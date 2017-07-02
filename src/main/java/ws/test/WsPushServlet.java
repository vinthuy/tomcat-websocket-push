package ws.test;

import com.alibaba.fastjson.JSON;
import ws.WsContainer;
import ws.model.WsResult;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
@WebServlet("/wsPush")
public class WsPushServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WsResult wsResult = WsResult.buildSuccessWsResult(WsContainer.PushMsgType.OK.getTag(), 123);
        WsContainer.instance().getPushServer().sendText(JSON.toJSONString(wsResult));
        super.doGet(req, resp);
    }
}
