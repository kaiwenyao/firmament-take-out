package dev.kaiwen.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link JacksonObjectMapper} 单元测试.
 */
class JacksonObjectMapperTest {

  private JacksonObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new JacksonObjectMapper();
  }

  @Nested
  @DisplayName("LocalDate 序列化与反序列化")
  class LocalDateTest {

    @Test
    void serializeLocalDateUsesDefaultDateFormat() throws Exception {
      LocalDate date = LocalDate.of(2025, 1, 31);
      String json = objectMapper.writeValueAsString(date);
      assertEquals("\"2025-01-31\"", json);
    }

    @Test
    void deserializeLocalDateFromDefaultFormat() throws Exception {
      String json = "\"2025-01-31\"";
      LocalDate date = objectMapper.readValue(json, LocalDate.class);
      assertEquals(LocalDate.of(2025, 1, 31), date);
    }

    @Test
    void localDateRoundTrip() throws Exception {
      LocalDate original = LocalDate.of(2024, 12, 1);
      String json = objectMapper.writeValueAsString(original);
      LocalDate restored = objectMapper.readValue(json, LocalDate.class);
      assertEquals(original, restored);
    }
  }

  @Nested
  @DisplayName("LocalDateTime 序列化与反序列化")
  class LocalDateTimeTest {

    @Test
    void serializeLocalDateTimeUsesDefaultDateTimeFormat() throws Exception {
      LocalDateTime dateTime = LocalDateTime.of(2025, 1, 31, 14, 30);
      String json = objectMapper.writeValueAsString(dateTime);
      assertEquals("\"2025-01-31 14:30\"", json);
    }

    @Test
    void deserializeLocalDateTimeFromDefaultFormat() throws Exception {
      String json = "\"2025-01-31 14:30\"";
      LocalDateTime dateTime = objectMapper.readValue(json, LocalDateTime.class);
      assertEquals(LocalDateTime.of(2025, 1, 31, 14, 30), dateTime);
    }

    @Test
    void localDateTimeRoundTrip() throws Exception {
      LocalDateTime original = LocalDateTime.of(2024, 12, 1, 9, 0);
      String json = objectMapper.writeValueAsString(original);
      LocalDateTime restored = objectMapper.readValue(json, LocalDateTime.class);
      assertEquals(original, restored);
    }
  }

  @Nested
  @DisplayName("LocalTime 序列化与反序列化")
  class LocalTimeTest {

    @Test
    void serializeLocalTimeUsesDefaultTimeFormat() throws Exception {
      LocalTime time = LocalTime.of(14, 30, 45);
      String json = objectMapper.writeValueAsString(time);
      assertEquals("\"14:30:45\"", json);
    }

    @Test
    void deserializeLocalTimeFromDefaultFormat() throws Exception {
      String json = "\"14:30:45\"";
      LocalTime time = objectMapper.readValue(json, LocalTime.class);
      assertEquals(LocalTime.of(14, 30, 45), time);
    }

    @Test
    void localTimeRoundTrip() throws Exception {
      LocalTime original = LocalTime.of(9, 5, 0);
      String json = objectMapper.writeValueAsString(original);
      LocalTime restored = objectMapper.readValue(json, LocalTime.class);
      assertEquals(original, restored);
    }
  }

  @Nested
  @DisplayName("Long 类型序列化为字符串（避免 JS 精度丢失）")
  class LongSerializationTest {

    @Test
    void serializeLongAsString() throws Exception {
      long value = 1234567890123456789L;
      String json = objectMapper.writeValueAsString(value);
      assertEquals("\"1234567890123456789\"", json);
    }

    @Test
    void serializeLongObjectAsString() throws Exception {
      Long value = 8076543210987654321L;
      String json = objectMapper.writeValueAsString(value);
      assertEquals("\"8076543210987654321\"", json);
    }

    @Test
    void deserializeLongFromString() throws Exception {
      String json = "\"1234567890123456789\"";
      Long value = objectMapper.readValue(json, Long.class);
      assertEquals(1234567890123456789L, value);
    }
  }

  @Nested
  @DisplayName("BigInteger 类型序列化为字符串")
  class BigIntegerSerializationTest {

    @Test
    void serializeBigIntegerAsString() throws Exception {
      BigInteger value = new BigInteger("123456789012345678901234567890");
      String json = objectMapper.writeValueAsString(value);
      assertEquals("\"123456789012345678901234567890\"", json);
    }

    @Test
    void deserializeBigIntegerFromString() throws Exception {
      String json = "\"123456789012345678901234567890\"";
      BigInteger value = objectMapper.readValue(json, BigInteger.class);
      assertEquals(new BigInteger("123456789012345678901234567890"), value);
    }
  }

  @Nested
  @DisplayName("未知属性不报错（FAIL_ON_UNKNOWN_PROPERTIES = false）")
  class UnknownPropertiesTest {

    @Test
    void deserializeWithUnknownPropertyDoesNotThrow() throws Exception {
      String json = "{\"name\":\"test\",\"unknownField\":\"ignored\"}";
      SimpleBean bean = objectMapper.readValue(json, SimpleBean.class);
      assertNotNull(bean);
      assertEquals("test", bean.name());
    }

    @Test
    void deserializeWithOnlyUnknownPropertiesReturnsDefaultValues() throws Exception {
      String json = "{\"extra\":\"value\"}";
      SimpleBean bean = objectMapper.readValue(json, SimpleBean.class);
      assertNotNull(bean);
      assertEquals(null, bean.name());
    }
  }

  @Nested
  @DisplayName("常量格式")
  class FormatConstantsTest {

    @Test
    void defaultDateFormatMatchesExpectedPattern() {
      assertEquals("yyyy-MM-dd", JacksonObjectMapper.DEFAULT_DATE_FORMAT);
    }

    @Test
    void defaultDateTimeFormatMatchesExpectedPattern() {
      assertEquals("yyyy-MM-dd HH:mm", JacksonObjectMapper.DEFAULT_DATE_TIME_FORMAT);
    }

    @Test
    void defaultTimeFormatMatchesExpectedPattern() {
      assertEquals("HH:mm:ss", JacksonObjectMapper.DEFAULT_TIME_FORMAT);
    }
  }

  /** 用于未知属性测试的简单 Bean. */
  record SimpleBean(String name) {}

}
