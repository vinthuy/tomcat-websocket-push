package ws;

import java.util.ArrayList;
import java.util.List;

public class PushMsgType {

    private static List<PushMsgType> list = new ArrayList<PushMsgType>();

    public PushMsgType(int tag) {
        this.tag = tag;
        list.add(this);
    }

    private int tag;

    public int getTag() {
        return tag;
    }


    public static PushMsgType OK = new PushMsgType(-100);

    public static PushMsgType getPushMsgType(int f) {
        for (PushMsgType pushMsgType : list) {
            if (pushMsgType.getTag() == f) {
                return pushMsgType;
            }
        }
        return null;
    }
}
