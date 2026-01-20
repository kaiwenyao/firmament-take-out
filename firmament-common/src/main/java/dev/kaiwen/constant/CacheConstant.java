package dev.kaiwen.constant;

/**
 * Cache key constants.
 */
public class CacheConstant {

  /**
   * Dish cache key prefix.
   */
  public static final String DISH_KEY_PREFIX = "dish_";

  /**
   * Refresh token Redis key prefix.
   */
  public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

  private CacheConstant() {
    // 工具类，禁止实例化
  }
}
