package dev.kaiwen.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import java.io.ByteArrayInputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云OSS工具类.
 */
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

  private String endpoint;
  private String accessKeyId;
  private String accessKeySecret;
  private String bucketName;

  /**
   * 文件上传.
   *
   * @param bytes      文件字节数组
   * @param objectName 对象名称（文件路径）
   * @return 文件访问URL
   */
  public String upload(byte[] bytes, String objectName) {

    // 创建OSSClient实例
    try (OssClientHolder holder = new OssClientHolder(endpoint, accessKeyId, accessKeySecret)) {
      OSS ossClient = holder.getClient();
      // 创建PutObject请求
      ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
    } catch (OSSException oe) {
      log.error("OSS异常：请求被OSS拒绝。错误消息: {}, 错误代码: {}, 请求ID: {}, 主机ID: {}",
          oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId(), oe);
      throw oe;
    } catch (ClientException ce) {
      log.error("OSS客户端异常：客户端在尝试与OSS通信时遇到严重内部问题。错误消息: {}",
          ce.getMessage(), ce);
      throw ce;
    }

    // 文件访问路径规则 https://BucketName.Endpoint/ObjectName
    StringBuilder stringBuilder = new StringBuilder("https://");
    stringBuilder
        .append(bucketName)
        .append(".")
        .append(endpoint)
        .append("/")
        .append(objectName);

    log.info("文件上传到:{}", stringBuilder);

    return stringBuilder.toString();
  }

  private static final class OssClientHolder implements AutoCloseable {
    private final OSS client;

    private OssClientHolder(String endpoint, String accessKeyId, String accessKeySecret) {
      this.client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    private OSS getClient() {
      return client;
    }

    @Override
    public void close() {
      client.shutdown();
    }
  }
}
