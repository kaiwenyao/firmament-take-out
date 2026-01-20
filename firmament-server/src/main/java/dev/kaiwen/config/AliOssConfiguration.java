package dev.kaiwen.config;

import dev.kaiwen.properties.AliOssProperties;
import dev.kaiwen.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置类.
 * 用于配置和创建阿里云OSS工具类Bean.
 */
@Configuration
@Slf4j
public class AliOssConfiguration {

  /**
   * 创建阿里云OSS工具类Bean.
   *
   * @param aliOssProperties 阿里云OSS配置属性
   * @return 阿里云OSS工具类实例
   */
  @Bean
  @ConditionalOnMissingBean
  public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
    log.info("创建阿里云上传工具对象: {}", aliOssProperties);
    return new AliOssUtil(aliOssProperties.getEndpoint(),
        aliOssProperties.getAccessKeyId(),
        aliOssProperties.getAccessKeySecret(),
        aliOssProperties.getBucketName());
  }
}
