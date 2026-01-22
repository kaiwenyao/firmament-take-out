package dev.kaiwen.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import sun.misc.Unsafe;

class PasswordUtilTest {

  private BCryptPasswordEncoder originalEncoder;

  @AfterEach
  void restorePasswordEncoder() throws Exception {
    // 恢复原始的passwordEncoder
    if (originalEncoder != null) {
      setFinalStaticField(PasswordUtil.class.getDeclaredField("passwordEncoder"), originalEncoder);
      originalEncoder = null;
    }
  }

  private void setFinalStaticField(Field field, Object newValue) throws Exception {
    field.setAccessible(true);
    
    // 使用Unsafe来修改final字段
    Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    Unsafe unsafe = (Unsafe) unsafeField.get(null);
    
    Object staticFieldBase = unsafe.staticFieldBase(field);
    long staticFieldOffset = unsafe.staticFieldOffset(field);
    unsafe.putObject(staticFieldBase, staticFieldOffset, newValue);
  }

  @Test
  void encodeAddsBcryptPrefixAndMatchesRawPassword() {
    // BCrypt hash should be prefixed and match the raw password.
    String raw = "p@ssw0rd";
    String encoded = PasswordUtil.encode(raw);

    assertTrue(encoded.startsWith("{BCRYPT}"));
    assertFalse(PasswordUtil.mismatches(raw, encoded));
    assertTrue(PasswordUtil.mismatches("wrong", encoded));
  }

  @Test
  void mismatchesHandlesMd5WithPrefix() {
    // MD5 prefix should be detected and validated against the raw password.
    String raw = "abc123";
    String md5 = md5Hex(raw);
    String encoded = "{MD5}" + md5;

    assertFalse(PasswordUtil.mismatches(raw, encoded));
    assertTrue(PasswordUtil.mismatches("wrong", encoded));
    assertTrue(PasswordUtil.isMd5(encoded));
  }

  @Test
  void mismatchesHandlesLegacyMd5WithoutPrefix() {
    // Legacy MD5 without prefix should still be treated as MD5.
    String raw = "legacy";
    String encoded = md5Hex(raw);

    assertFalse(PasswordUtil.mismatches(raw, encoded));
    assertTrue(PasswordUtil.mismatches("wrong", encoded));
    assertTrue(PasswordUtil.isMd5(encoded));
  }

  @Test
  void mismatchesReturnsTrueForNullInputs() {
    // Null values should be treated as mismatch to stay safe by default.
    assertTrue(PasswordUtil.mismatches(null, "anything"));
    assertTrue(PasswordUtil.mismatches("anything", null));
    assertTrue(PasswordUtil.mismatches(null, null));
    assertFalse(PasswordUtil.isMd5(null));
  }

  @Test
  void mismatchesReturnsTrueForUnknownFormat() {
    // Unknown/invalid formats should be treated as mismatches.
    assertTrue(PasswordUtil.mismatches("pw", "not-a-hash"));
  }

  @Test
  void mismatchesHandlesBcryptWithoutPrefix() {
    // BCrypt hash without prefix should be validated correctly.
    // This covers the code path at lines 79-85
    String raw = "testPassword123";
    String encodedWithPrefix = PasswordUtil.encode(raw);
    // 去掉BCRYPT前缀，模拟不带前缀的BCrypt hash
    String encodedWithoutPrefix = encodedWithPrefix.substring("{BCRYPT}".length());

    // 验证成功的情况：密码匹配
    assertFalse(PasswordUtil.mismatches(raw, encodedWithoutPrefix));
    // 验证失败的情况：密码不匹配
    assertTrue(PasswordUtil.mismatches("wrongPassword", encodedWithoutPrefix));
  }

  @Test
  void mismatchesHandlesInvalidBcryptFormat() {
    // Invalid BCrypt format (not 32 chars, not prefixed) should handle exception gracefully.
    // This tests the exception handling in the try-catch block (lines 80-84)
    String raw = "testPassword";
    // 创建一个长度不是32位且格式错误的hash（不是有效的BCrypt格式）
    String invalidBcrypt = "invalid-bcrypt-hash-format-that-is-not-32-chars";

    // 应该返回true（不匹配），因为格式错误会触发异常处理
    assertTrue(PasswordUtil.mismatches(raw, invalidBcrypt));
  }

  @Test
  void mismatchesHandlesBcryptWithoutPrefixForDifferentLengths() {
    // Test BCrypt without prefix for various password lengths
    // This ensures the code path at lines 79-85 is covered for different scenarios
    String[] testPasswords = {"short", "mediumLengthPassword", "veryLongPassword123456789"};
    
    for (String raw : testPasswords) {
      String encodedWithPrefix = PasswordUtil.encode(raw);
      String encodedWithoutPrefix = encodedWithPrefix.substring("{BCRYPT}".length());
      
      // 验证正确密码应该匹配
      assertFalse(PasswordUtil.mismatches(raw, encodedWithoutPrefix));
      // 验证错误密码应该不匹配
      assertTrue(PasswordUtil.mismatches(raw + "wrong", encodedWithoutPrefix));
    }
  }

  @Test
  void mismatchesHandlesExceptionInBcryptVerification() throws Exception {
    // 测试异常处理分支（第82-84行）
    // 使用反射和Mock来模拟passwordEncoder.matches()抛出异常的情况
    Field field = PasswordUtil.class.getDeclaredField("passwordEncoder");
    
    // 保存原始encoder
    field.setAccessible(true);
    originalEncoder = (BCryptPasswordEncoder) field.get(null);
    
    // 创建Mock encoder，模拟抛出异常
    BCryptPasswordEncoder mockEncoder = mock(BCryptPasswordEncoder.class);
    when(mockEncoder.matches(anyString(), anyString())).thenThrow(new RuntimeException("模拟的异常"));
    
    // 使用Unsafe替换passwordEncoder（final字段）
    setFinalStaticField(field, mockEncoder);
    
    // 测试：当passwordEncoder.matches()抛出异常时，应该返回true（不匹配）
    String raw = "testPassword";
    String encoded = "some-bcrypt-hash-without-prefix-not-32-chars";
    
    // 应该返回true，因为异常被捕获并返回true
    assertTrue(PasswordUtil.mismatches(raw, encoded));
  }

  @Test
  void isMd5Handles32CharStringWithoutBcryptPrefix() {
    // 测试第99行的条件：长度为32且不以{BCRYPT}开头的字符串
    // 这种情况应该被识别为MD5格式
    
    // 情况1：长度为32，不以{BCRYPT}开头，也不以{MD5}开头 - 应该返回true
    String md5Hash32 = "0123456789abcdef0123456789abcdef"; // 32位十六进制字符串
    assertTrue(PasswordUtil.isMd5(md5Hash32));
    
    // 情况2：长度为32，以{BCRYPT}开头 - 应该返回false（虽然BCrypt通常是60位，但测试边界情况）
    String bcryptPrefix32 = "{BCRYPT}0123456789abcdef01234567"; // 32位（包含前缀）
    assertFalse(PasswordUtil.isMd5(bcryptPrefix32));
    
    // 情况3：长度不为32，不以{BCRYPT}开头 - 应该返回false（如果也不以{MD5}开头）
    String shortString = "short";
    assertFalse(PasswordUtil.isMd5(shortString));
    
    // 情况4：长度不为32，以{BCRYPT}开头 - 应该返回false
    String bcryptHash = "{BCRYPT}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"; // 典型的BCrypt hash
    assertFalse(PasswordUtil.isMd5(bcryptHash));
    
    // 情况5：长度为32，以{MD5}开头 - 应该返回true（走第一个条件）
    String md5WithPrefix = "{MD5}" + md5Hash32;
    assertTrue(PasswordUtil.isMd5(md5WithPrefix));
  }

  @Test
  void isMd5HandlesVariousLengthStrings() {
    // 测试不同长度的字符串，确保第99行的条件分支都被覆盖
    
    // 长度为31的字符串 - 应该返回false（除非以{MD5}开头）
    String length31 = "0123456789abcdef0123456789abcde"; // 31位
    assertFalse(PasswordUtil.isMd5(length31));
    
    // 长度为33的字符串 - 应该返回false（除非以{MD5}开头）
    String length33 = "0123456789abcdef0123456789abcdef0"; // 33位
    assertFalse(PasswordUtil.isMd5(length33));
    
    // 长度为32，但以{BCRYPT}开头（虽然不太可能，但测试边界情况）
    // 注意：{BCRYPT}本身是8个字符，所以如果总长度是32，那么hash部分只有24位
    String bcrypt32 = "{BCRYPT}0123456789abcdef012345"; // 总长度32
    assertFalse(PasswordUtil.isMd5(bcrypt32));
  }

  private static String md5Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      // MD5 is guaranteed by the JDK; rethrow if the environment is broken.
      throw new IllegalStateException("MD5 not available", e);
    }
  }
}
