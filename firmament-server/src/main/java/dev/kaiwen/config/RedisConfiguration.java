package dev.kaiwen.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import dev.kaiwen.json.JacksonObjectMapper;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类.
 * 配置RedisTemplate和CacheManager，支持对象序列化和类型安全.
 */
@Configuration
@Slf4j
public class RedisConfiguration {

  /**
   * 配置RedisTemplate（Object key, Object value）.
   * 用于通用的Redis操作，支持对象序列化并开启类型白名单.
   *
   * @param redisConnectionFactory Redis连接工厂
   * @return RedisTemplate实例
   */
  @Bean
  public RedisTemplate<Object, Object> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);

    GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(jsonSerializer);
    redisTemplate.setHashValueSerializer(jsonSerializer);

    return redisTemplate;
  }

  /**
   * RedisTemplate for String key and Object value.
   * Used for caching dish lists and other objects.
   *
   * @param redisConnectionFactory Redis连接工厂
   * @return RedisTemplate实例（String key, Object value）
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplateStringObject(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);

    GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(jsonSerializer);
    redisTemplate.setHashValueSerializer(jsonSerializer);

    return redisTemplate;
  }

  /**
   * RedisTemplate for String key and String value. Used for storing refresh tokens and other string
   * values.
   */
  @Bean
  public RedisTemplate<String, String> redisTemplateStringString(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);

    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new StringRedisSerializer());

    return redisTemplate;
  }

  /**
   * 配置Redis缓存管理器.
   * 用于Spring Cache注解，支持对象序列化和类型安全，缓存过期时间为1小时.
   *
   * @param redisConnectionFactory Redis连接工厂
   * @return CacheManager实例
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
    GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1))
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
            jsonSerializer)) // 使用带类型记录的 Serializer
        .disableCachingNullValues();

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(config)
        .build();
  }

  /**
   * 创建带类型白名单的JSON序列化器.
   * 开启类型白名单的作用：在生成JSON时，添加"@class"字段来记录类名，确保反序列化时类型安全.
   *
   * @return GenericJackson2JsonRedisSerializer实例
   */
  private GenericJackson2JsonRedisSerializer createJsonSerializer() {
    JacksonObjectMapper objectMapper = new JacksonObjectMapper();
    objectMapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );
    return new GenericJackson2JsonRedisSerializer(objectMapper);
  }
}