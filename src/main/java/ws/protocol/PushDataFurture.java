package ws.protocol;

/**
 * Created by ruiyong.hry on 03/08/2017.
 */
public interface PushDataFurture {


    /**
     * ∑µªÿ«Î«Û±‡∫≈
     */
    String requestId();


    boolean isDone();

    void done();


    Object get() throws InterruptedException;


    Object get(long timeout) throws   InterruptedException;


}
