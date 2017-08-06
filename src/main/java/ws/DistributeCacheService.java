package ws;


import java.io.Serializable;
import java.util.Map;

public interface DistributeCacheService {

    public void put(String key, Serializable object, int expiredTime);

    public Serializable get(String key);

    public void remove(String key);

    Integer incr(String key, int size, int defaultV, int expiredTime);


    public void putGroup(String group, String key, Serializable object, int ExpireTime);

    public Map<String, Serializable> getGroup(String group);

    public void removeGroup(String group);

    public boolean setCount(String key, int count, int expiredTime);

    public Serializable gutGroupObj(String group, String key);

    public void invalidGroup(String group, String key);

    public void prefixIncr(String group, String key, int size, int defaultV, int expirdTime);

    public void prefixDecr(String group, String key, int size, int defaultV, int expirdTime);

    public boolean prefixSetCount(String group, String key, int count, int expiredTime);

    void prefixDelete(String group, String key);
}
