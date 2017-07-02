package ws.test;

import ws.WsContainer;
import ws.client.WsProxyClient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
@WebServlet("/wsClient")
public class WsClientServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WsContainer.initClient("ws://localhost:8081/pushClient.ws");
        WsProxyClient wsProxyClient = WsContainer.instance().getWsProxyClient();
        wsProxyClient.sendText("hello");
        super.doGet(req, resp);
    }
}
