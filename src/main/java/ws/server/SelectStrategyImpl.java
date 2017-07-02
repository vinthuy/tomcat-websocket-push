package ws.server;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
public class SelectStrategyImpl implements SelectStrategy {

    public volatile int lastSelect = 0;

    public int select(int size) {
        int canSelectIndex = size - 1;
        int res = -1;
        if (canSelectIndex < 0) {
            res = -1;
        } else if (canSelectIndex == 0) {
            res = 0;
        } else if (lastSelect < canSelectIndex) {
            //·µ»ØÔÚ¼Ó+1
            res = lastSelect;
            lastSelect++;
        }

        //lastSelect >= canSelectIndex-1;
        else if (lastSelect > canSelectIndex) {
            lastSelect = 0;
            res = lastSelect;
        } else {
            res = lastSelect;
            lastSelect = 0;
        }
        return res;
    }

//    public static void main(String[] args) {
//        SelectStrategeImpl selectStratege = new SelectStrategeImpl();
//        System.out.println(selectStratege.select(6)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(6)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(6)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(6)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(6)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(6)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(6)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(3)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(2)+">>>"+selectStratege.lastSelect);
//        System.out.println(selectStratege.select(8)+">>>"+selectStratege.lastSelect);
//    }
}

