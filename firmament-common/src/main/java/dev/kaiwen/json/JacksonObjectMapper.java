package dev.kaiwen.json;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 对象映射器:基于jackson将Java对象转为json，或者将json转为Java对象. 将JSON解析为Java对象的过程称为 [从JSON反序列化Java对象].
 * 从Java对象生成JSON的过程称为 [序列化Java对象到JSON].
 */
public class JacksonObjectMapper extends ObjectMapper {

  public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
  public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
  public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

  /**
   * 构造对象映射器，配置日期时间序列化和反序列化格式.
   */
  public JacksonObjectMapper() {
    super();
    // 收到未知属性时不报异常
    this.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    SimpleModule simpleModule = new SimpleModule()
        .addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
        .addDeserializer(LocalDate.class,
            new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
        .addDeserializer(LocalTime.class,
            new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
        .addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
        .addSerializer(LocalDate.class,
            new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
        .addSerializer(LocalTime.class,
            new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
        // 解决 JavaScript 中 Long 类型大数精度丢失问题：将 Long 类型序列化为字符串
        // 4. 【优化点3】补全 BigInteger 的精度处理
        // 除了 Long，BigInteger 在 JS 中也会丢失精度，建议一并处理
        .addSerializer(BigInteger.class, ToStringSerializer.instance)
        .addSerializer(Long.class, ToStringSerializer.instance)
        .addSerializer(Long.TYPE, ToStringSerializer.instance);

    // 注册功能模块 例如，可以添加自定义序列化器和反序列化器
    this.registerModule(simpleModule);
  }
}
