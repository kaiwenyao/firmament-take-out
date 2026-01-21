package dev.kaiwen.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

class PasswordUtilTest {

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
