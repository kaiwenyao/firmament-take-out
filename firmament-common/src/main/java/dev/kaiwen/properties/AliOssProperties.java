package dev.kaiwen.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置属性.
 */
@Component
@ConfigurationProperties(prefix = "firmament.alioss")
@Data
public class AliOssProperties {

  private String endpoint;
  private String bucketName;
  private String region;
  private String accessKeyId;
  private String accessKeySecret;

}
