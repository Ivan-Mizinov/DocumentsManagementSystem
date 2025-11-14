package db.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCacheUtil {
    private static final JedisPoolConfig POOL_CONFIG = buildPoolConfig();
    private static final JedisPool JEDIS_POOL = new JedisPool(POOL_CONFIG, "localhost", 6379);
    private static final ObjectMapper OBJECT_MAPPER = buildObjectMapper();

    private RedisCacheUtil() {
    }

    private static JedisPoolConfig buildPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(16);
        config.setMaxIdle(8);
        config.setMinIdle(0);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        return config;
    }

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    private static <T> T executeWithJedis(String key, JedisAction<T> action) {
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            return action.apply(jedis, key);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка работы с Redis по ключу " + key, e);
        }
    }

    @FunctionalInterface
    private interface JedisAction<T> {
        T apply(Jedis jedis, String key) throws Exception;
    }

    public static <T> void cacheValue(String key, T value) {
        executeWithJedis(key, (jedis, k) -> {
            jedis.set(k, OBJECT_MAPPER.writeValueAsString(value));
            return null;
        });
    }

    public static <T> T getValue(String key, Class<T> clazz) {
        return executeWithJedis(key, (jedis, k) -> {
            String value = jedis.get(k);
            return value == null ? null : OBJECT_MAPPER.readValue(value, clazz);
        });
    }

    public static <T> T getValue(String key, TypeReference<T> type) {
        return executeWithJedis(key, (jedis, k) -> {
            String value = jedis.get(k);
            return value == null ? null : OBJECT_MAPPER.readValue(value, type);
        });
    }

    public static void evict(String key) {
        executeWithJedis(key, (jedis, k) -> {
            jedis.del(k);
            return null;
        });
    }

    public static void shutdown() {
        JEDIS_POOL.close();
    }
}
