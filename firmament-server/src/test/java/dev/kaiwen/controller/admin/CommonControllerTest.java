package dev.kaiwen.controller.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.handler.GlobalExceptionHandler;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.utils.AliOssUtil;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@WebMvcTest(CommonController.class)
class CommonControllerTest {

  @MockitoBean
  private AliOssUtil aliOssUtil;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private MockMvc mockMvc;

  private void setupJwtTokenMock() {
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn("1");
  }

  @Test
  void uploadSuccess() throws Exception {
    setupJwtTokenMock();

    MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg",
        "image content".getBytes());

    given(aliOssUtil.upload(any(byte[].class), anyString())).willReturn(
        "https://bucket.oss.com/path/uuid.jpg");

    mockMvc.perform(
            multipart("/admin/common/upload").file(file).header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value("https://bucket.oss.com/path/uuid.jpg"));

    verify(aliOssUtil).upload(any(byte[].class), anyString());
  }

  @ParameterizedTest
  @MethodSource("uploadInvalidFileCases")
  void uploadWhenInvalidFile(UploadCase uploadCase) throws Exception {
    setupJwtTokenMock();

    MockMultipartHttpServletRequestBuilder requestBuilder = multipart("/admin/common/upload");
    if (uploadCase.file() != null) {
      requestBuilder.file(uploadCase.file());
    }
    requestBuilder.header("token", "mock-accessToken");

    mockMvc.perform(requestBuilder)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.msg").value(uploadCase.expectedMessage()));
  }

  @Test
  void uploadWhenUnsupportedExtension() throws Exception {
    setupJwtTokenMock();

    MockMultipartFile file = new MockMultipartFile("file", "test.svg", "image/svg+xml",
        "content".getBytes());

    mockMvc.perform(
            multipart("/admin/common/upload").file(file).header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.msg").value(containsString("不支持的文件类型")));
  }

  @Test
  void uploadWhenFileTooLarge() throws Exception {
    setupJwtTokenMock();

    byte[] largeContent = new byte[(int) (51L * 1024 * 1024)];
    MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeContent);

    mockMvc.perform(
            multipart("/admin/common/upload").file(file).header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.msg").value("文件大小不能超过50MB"));
  }

  @Test
  void uploadWhenOssThrowsException() throws Exception {
    setupJwtTokenMock();

    MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg",
        "content".getBytes());
    given(aliOssUtil.upload(any(byte[].class), anyString())).willThrow(
        new RuntimeException("OSS error"));

    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(
              multipart("/admin/common/upload").file(file).header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0));
    } finally {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void uploadWhenOssThrowsIoException() throws Exception {
    setupJwtTokenMock();

    MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg",
        "content".getBytes());
    given(aliOssUtil.upload(any(byte[].class), anyString())).willThrow(new IOException("网络异常"));

    Logger logger = (Logger) LoggerFactory.getLogger(CommonController.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(
              multipart("/admin/common/upload").file(file).header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0))
          .andExpect(jsonPath("$.msg").value("文件上传失败"));
    } finally {
      logger.setLevel(originalLevel);
    }
  }

  private static Stream<UploadCase> uploadInvalidFileCases() {
    return Stream.of(
        new UploadCase(null, "文件不能为空"),
        new UploadCase(
            new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]), "文件不能为空"),
        new UploadCase(
            new MockMultipartFile("file", null, "image/jpeg", "content".getBytes()),
            "文件名不能为空"),
        new UploadCase(
            new MockMultipartFile("file", "", "image/jpeg", "content".getBytes()),
            "文件名不能为空"),
        new UploadCase(
            new MockMultipartFile("file", "   ", "image/jpeg", "content".getBytes()),
            "文件名不能为空"),
        new UploadCase(
            new MockMultipartFile("file", "\t\n  ", "image/jpeg", "content".getBytes()),
            "文件名不能为空"),
        new UploadCase(
            new MockMultipartFile("file", "../../etc/passwd.jpg", "image/jpeg",
                "content".getBytes()),
            "文件名包含非法字符"),
        new UploadCase(
            new MockMultipartFile("file", "path/file.jpg", "image/jpeg", "content".getBytes()),
            "文件名包含非法字符"),
        new UploadCase(
            new MockMultipartFile("file", "path\\file.jpg", "image/jpeg", "content".getBytes()),
            "文件名包含非法字符"),
        new UploadCase(
            new MockMultipartFile("file", "noExt", "image/jpeg", "content".getBytes()),
            "文件必须有扩展名"),
        new UploadCase(
            new MockMultipartFile("file", "file.", "image/jpeg", "content".getBytes()),
            "文件必须有扩展名")
    );
  }

  private record UploadCase(MockMultipartFile file, String expectedMessage) {

  }
}
