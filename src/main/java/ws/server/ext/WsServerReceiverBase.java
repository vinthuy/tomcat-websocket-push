package ws.server.ext;

import ws.WsConstants;
import ws.WsContainerSingle;
import ws.model.WsResult;
import ws.serialize.SerializeManager;
import ws.server.WsServerReceiver;
import ws.util.HostServerUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by ruiyong.hry on 09/08/2017.
 */
public class WsServerReceiverBase implements WsServerReceiver {

    @Override
    public void receive(InputStream requestInstream, OutputStream outputStream) throws Exception {

        SerializeManager serializeManager = WsContainerSingle.instance().getSerializeManager();
        WsServerInterResult wsServerInterResult = null;
        if (serializeManager == null) {
            wsServerInterResult = WsServerInterResult.buildFailureResult(String.format("服务器没有启动,%s:%s", new Object[]{HostServerUtil.getLocalIp(), HostServerUtil.getPort()}));
            writeResult(serializeManager, wsServerInterResult, outputStream);
            return;
        }
        byte[] requestBytes = null;
        try {
            requestBytes = IOUtils.toByteArray(requestInstream);
        } catch (IOException e) {
            WsConstants.wslogger.error(e.getMessage(), e);
        }
        if (ArrayUtils.isEmpty(requestBytes)) {
            wsServerInterResult = WsServerInterResult.buildFailureResult("参数错误");
            writeResult(serializeManager, wsServerInterResult, outputStream);
            return;
        }

        Map param = (Map) serializeManager.deserialize(requestBytes);

        String key = MapUtils.getString(param, "key");
        Object message = MapUtils.getObject(param, "message");
        if (StringUtils.isBlank(key) || message == null) {
            wsServerInterResult = WsServerInterResult.buildFailureResult("key|message is null");
            writeResult(serializeManager, wsServerInterResult, outputStream);
            return;
        }

        boolean sync = MapUtils.getBoolean(param, "sync", false);
        if (message instanceof WsResult) {
            WsConstants.wslogger.warn(String.format("receive push api key:%s ", key));
        }
        Object data = null;
        try {
            data = WsContainerSingle.instance().getPushServer().executeSendLocal(key, message, sync);
        } catch (Exception e) {
            WsConstants.wslogger.error(e.getMessage(), e);
            wsServerInterResult = WsServerInterResult.buildFailureResult(e.getMessage());
            writeResult(serializeManager, wsServerInterResult, outputStream);
            return;
        }
//        if (data == null) {
//            data = "ok";
//        }
        wsServerInterResult = WsServerInterResult.buildSuccessResult(data);
        writeResult(serializeManager, wsServerInterResult, outputStream);
    }

    private void writeResult(SerializeManager serializeManager, WsServerInterResult wsServerInterResult, OutputStream outputStream) throws Exception {
        byte[] responseBytes = serializeManager.serialize(wsServerInterResult);
        IOUtils.write(responseBytes, outputStream);
    }

}
