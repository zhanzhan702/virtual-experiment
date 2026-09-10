package com.example.experiment.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 插件配置。 */
@Configuration
public class MybatisPlusConfig {

  /**
   * 注册分页插件。不注册时 {@code Page} 查询不会追加 LIMIT，会一次性查出全表。
   *
   * <p>{@code setMaxLimit(200)} 限制单页最大条数，防止前端传入 size=999999 拖垮数据库。
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
    pagination.setMaxLimit(200L);
    interceptor.addInnerInterceptor(pagination);
    return interceptor;
  }
}
