package dev.kaiwen.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

  @Mock
  private BCryptPasswordEncoder passwordEncoder;

  private PasswordService passwordService;

  @BeforeEach
  void setUp() {
    // 使用构造函数注入 Mock 的 passwordEncoder
    passwordService = new PasswordService(passwordEncoder);
  }

  @Test
  void encodeAddsBcryptPrefixAndMatchesRawPassword() {
    // BCrypt hash should be prefixed and match the raw password.
    String raw = "p@ssw0rd";
    String bcryptHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    
    when(passwordEncoder.encode(anyString())).thenReturn(bcryptHash);
    when(passwordEncoder.matches(raw, bcryptHash)).thenReturn(true);
    when(passwordEncoder.matches("wrong", bcryptHash)).thenReturn(false);
    
    String encoded = passwordService.encode(raw);
    assertTrue(encoded.startsWith("{BCRYPT}"));
    assertFalse(passwordService.mismatches(raw, encoded));
    assertTrue(passwordService.mismatches("wrong", encoded));
  }

  @Test
  void mismatchesHandlesMd5WithPrefix() {
    // MD5 prefix should be detected and validated against the raw password.
    String raw = "abc123";
    String md5 = md5Hex(raw);
    String encoded = "{MD5}" + md5;

    assertFalse(passwordService.mismatches(raw, encoded));
    assertTrue(passwordService.mismatches("wrong", encoded));
    assertTrue(passwordService.isMd5(encoded));
  }

  @Test
  void mismatchesHandlesLegacyMd5WithoutPrefix() {
    // Legacy MD5 without prefix should still be treated as MD5.
    String raw = "legacy";
    String encoded = md5Hex(raw);

    assertFalse(passwordService.mismatches(raw, encoded));
    assertTrue(passwordService.mismatches("wrong", encoded));
    assertTrue(passwordService.isMd5(encoded));
  }

  @Test
  void mismatchesReturnsTrueForNullInputs() {
    // Null values should be treated as mismatch to stay safe by default.
    assertTrue(passwordService.mismatches(null, "anything"));
    assertTrue(passwordService.mismatches("anything", null));
    assertTrue(passwordService.mismatches(null, null));
    assertFalse(passwordService.isMd5(null));
  }

  @Test
  void mismatchesReturnsTrueForUnknownFormat() {
    // Unknown/invalid formats should be treated as mismatches.
    assertTrue(passwordService.mismatches("pw", "not-a-hash"));
  }

  @Test
  void isMd5Handles32CharStringWithoutBcryptPrefix() {
    // 测试第99行的条件：长度为32且不以{BCRYPT}开头的字符串
    // 这种情况应该被识别为MD5格式

    // 情况1：长度为32，不以{BCRYPT}开头，也不以{MD5}开头 - 应该返回true
    String md5Hash32 = "0123456789abcdef0123456789abcdef"; // 32位十六进制字符串
    assertTrue(passwordService.isMd5(md5Hash32));

    // 情况2：长度为32，以{BCRYPT}开头 - 应该返回false（虽然BCrypt通常是60位，但测试边界情况）
    String bcryptPrefix32 = "{BCRYPT}0123456789abcdef01234567"; // 32位（包含前缀）
    assertFalse(passwordService.isMd5(bcryptPrefix32));

    // 情况3：长度不为32，不以{BCRYPT}开头 - 应该返回false（如果也不以{MD5}开头）
    String shortString = "short";
    assertFalse(passwordService.isMd5(shortString));

    // 情况4：长度不为32，以{BCRYPT}开头 - 应该返回false
    String bcryptHash = "{BCRYPT}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    // 典型的BCrypt hash
    assertFalse(passwordService.isMd5(bcryptHash));

    // 情况5：长度为32，以{MD5}开头 - 应该返回true（走第一个条件）
    String md5WithPrefix = "{MD5}" + md5Hash32;
    assertTrue(passwordService.isMd5(md5WithPrefix));
  }

  @Test
  void isMd5HandlesVariousLengthStrings() {
    // 测试不同长度的字符串，确保第99行的条件分支都被覆盖

    // 长度为31的字符串 - 应该返回false（除非以{MD5}开头）
    String length31 = "0123456789abcdef0123456789abcde"; // 31位
    assertFalse(passwordService.isMd5(length31));

    // 长度为33的字符串 - 应该返回false（除非以{MD5}开头）
    String length33 = "0123456789abcdef0123456789abcdef0"; // 33位
    assertFalse(passwordService.isMd5(length33));

    // 长度为32，但以{BCRYPT}开头（虽然不太可能，但测试边界情况）
    // 注意：{BCRYPT}本身是8个字符，所以如果总长度是32，那么hash部分只有24位
    String bcrypt32 = "{BCRYPT}0123456789abcdef012345"; // 总长度32
    assertFalse(passwordService.isMd5(bcrypt32));
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
