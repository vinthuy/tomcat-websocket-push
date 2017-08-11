package ws.util;


import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianFieldException;
import com.caucho.hessian.io.SerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 使用Hessian2进行序列化和反序列化
 */
public class HessianUtils {

    private static final Logger logger = LoggerFactory.getLogger(HessianUtils.class);

    private static SerializerFactory serializerFactory = new SerializerFactory();

    static {
        serializerFactory.setAllowNonSerializable(true);
    }

    public static byte[] serialize(Object obj) throws Exception {
        byte[] bytes = new byte[0];
        if (obj == null) {
            return bytes;
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(os);
            output.setSerializerFactory(serializerFactory);
            output.writeObject(obj);
            output.flush();
            bytes = os.toByteArray();
            output.close();
            return bytes;
        } catch (Exception e) {
            throw new Exception("Doom hessian serialize error.", e);
        }
    }

    public static Object deserialize(byte[] bytes) throws Exception {
        Object obj = null;
        if (bytes == null || bytes.length == 0) {
            return obj;
        }
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            Hessian2Input input = new Hessian2Input(is);
            input.setSerializerFactory(serializerFactory);
            obj = input.readObject();
            input.close();
            return obj;
        } catch (Exception e) {
            if (e instanceof HessianFieldException) {
                logger.error("deserialize error!", e);
            }
            throw new Exception("Doom hessian deserialize error.", e);
        }
    }


    public static void setClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            serializerFactory = new SerializerFactory(classLoader);
            serializerFactory.setAllowNonSerializable(true);
        }
    }
}
