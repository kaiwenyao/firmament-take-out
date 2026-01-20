package dev.kaiwen.utils;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Http工具类.
 */
@Slf4j
public class HttpClientUtil {

  private static final String UTF_8 = "UTF-8";
  private static final String ERROR_CLOSE_RESPONSE = "关闭HTTP响应流失败";
  private static final String ERROR_CLOSE_CLIENT = "关闭HTTP客户端失败";

  private HttpClientUtil() {
    // 工具类，禁止实例化
  }

  /**
   * 发送GET方式请求.
   *
   * @param url     请求URL
   * @param paramMap 请求参数
   * @return 响应结果
   */
  public static String doGet(String url, Map<String, String> paramMap) {
    // 创建Httpclient对象
    CloseableHttpClient httpClient = HttpClients.createDefault();

    String result = "";
    CloseableHttpResponse response = null;

    try {
      URIBuilder builder = new URIBuilder(url);
      if (paramMap != null) {
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
          builder.addParameter(entry.getKey(), entry.getValue());
        }
      }
      URI uri = builder.build();

      // 创建GET请求
      HttpGet httpGet = new HttpGet(uri);

      // 发送请求
      response = httpClient.execute(httpGet);

      // 判断响应状态
      if (response != null && response.getStatusLine().getStatusCode() == 200) {
        result = EntityUtils.toString(response.getEntity(), UTF_8);
      }
    } catch (Exception e) {
      log.error("HTTP GET请求失败: {}", url, e);
    } finally {
      closeResources(response, httpClient);
    }

    return result;
  }

  /**
   * 安全关闭HTTP资源.
   *
   * @param response   HTTP响应
   * @param httpClient HTTP客户端
   */
  private static void closeResources(CloseableHttpResponse response,
      CloseableHttpClient httpClient) {
    if (response != null) {
      try {
        response.close();
      } catch (IOException e) {
        log.error(ERROR_CLOSE_RESPONSE, e);
      }
    }
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (IOException e) {
        log.error(ERROR_CLOSE_CLIENT, e);
      }
    }
  }

}
