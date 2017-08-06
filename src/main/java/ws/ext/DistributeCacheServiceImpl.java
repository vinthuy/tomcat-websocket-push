package ws.ext;

import ws.DistributeCacheService;

import java.io.Serializable;
import java.util.Map;

/**
 * 需要自己实现
 * Created by ruiyong.hry on 06/08/2017.
 */
public class DistributeCacheServiceImpl implements DistributeCacheService {
    @Override
    public void put(String key, Serializable object, int expiredTime) {

    }

    @Override
    public Serializable get(String key) {
        return null;
    }

    @Override
    public void remove(String key) {

    }

    @Override
    public Integer incr(String key, int size, int defaultV, int expiredTime) {
        return null;
    }

    @Override
    public void putGroup(String group, String key, Serializable object, int ExpireTime) {

    }

    @Override
    public Map<String, Serializable> getGroup(String group) {
        return null;
    }

    @Override
    public void removeGroup(String group) {

    }

    @Override
    public boolean setCount(String key, int count, int expiredTime) {
        return false;
    }

    @Override
    public Serializable gutGroupObj(String group, String key) {
        return null;
    }

    @Override
    public void invalidGroup(String group, String key) {

    }

    @Override
    public void prefixIncr(String group, String key, int size, int defaultV, int expirdTime) {

    }

    @Override
    public void prefixDecr(String group, String key, int size, int defaultV, int expirdTime) {

    }

    @Override
    public boolean prefixSetCount(String group, String key, int count, int expiredTime) {
        return false;
    }

    @Override
    public void prefixDelete(String group, String key) {

    }
}
