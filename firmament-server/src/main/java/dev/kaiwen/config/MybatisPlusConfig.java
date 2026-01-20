package dev.kaiwen.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置类.
 * 用于配置MyBatis Plus的分页插件等拦截器.
 */
@Configuration
public class MybatisPlusConfig {

  /**
   * 配置MyBatis Plus拦截器.
   * 添加分页插件，支持MySQL数据库的分页查询.
   *
   * @return MyBatis Plus拦截器实例
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 添加分页插件
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
  }
}