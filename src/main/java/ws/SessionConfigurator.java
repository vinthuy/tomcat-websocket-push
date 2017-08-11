package ws;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

public class SessionConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec,
                                HandshakeRequest request, HandshakeResponse response) {
//        String ip = getRemoteHost(request);
//        if (StringUtils.isNotBlank(ip)) {
//            sec.getUserProperties().put(WsConstants.clinetHost, ip);
//        }
    }


    public String getRemoteHost(HandshakeRequest request) {
        Map<String, List<String>> headers = request.getHeaders();
        String ip = getHeader(headers, "x-real-ip");
        if (StringUtils.isBlank(ip)) {
            ip = getHeader(headers, "x-forwarded-for");
        }
        if (StringUtils.isBlank(ip)) {
            ip = getHeader(headers, "wl-proxy-client-ip");
        }
        if (StringUtils.isBlank(ip)) {
            ip = getHeader(headers, "ns-client-ip");
        }

        return ip;
    }

    private String getHeader(Map<String, List<String>> headers, String key) {
        List<String> values = headers.get(key);
        if (CollectionUtils.isNotEmpty(values)) {
            return values.get(0);
        }
        return StringUtils.EMPTY;
    }
}