package dev.kaiwen.controller.admin;

import dev.kaiwen.result.Result;
import dev.kaiwen.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static dev.kaiwen.constant.MessageConstant.UPLOAD_FAILED;

@RestController
@RequestMapping("/admin/common")
@Slf4j
@Tag(name = "通用接口")
@RequiredArgsConstructor
public class CommonController {
    private final AliOssUtil aliOssUtil;

    // 允许上传的图片文件扩展名白名单（禁用SVG以防止存储型XSS攻击）
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    // 单个文件最大大小限制（50MB，与配置文件保持一致）
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @PostMapping("/upload")
    @Operation(summary = "文件上传")
    public Result<String> upload(MultipartFile file)  {
        log.info("收到文件上传请求，文件名: {}", file != null ? file.getOriginalFilename() : "null");

        // 1. 基本验证：检查文件是否为空
        if (file == null || file.isEmpty()) {
            log.warn("文件上传失败：文件为空");
            return Result.error("文件不能为空");
        }

        // 2. 获取原始文件名并验证
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            log.warn("文件上传失败：文件名为空");
            return Result.error("文件名不能为空");
        }

        // 3. 安全检查：防止路径遍历攻击
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            log.warn("文件上传失败：文件名包含非法字符 - {}", originalFilename);
            return Result.error("文件名包含非法字符");
        }

        // 4. 提取并验证文件扩展名
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            log.warn("文件上传失败：文件缺少扩展名 - {}", originalFilename);
            return Result.error("文件必须有扩展名");
        }

        String extension = originalFilename.substring(lastDotIndex).toLowerCase();

        // 5. 文件类型白名单验证
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            log.warn("文件上传失败：不支持的文件类型 - {}", extension);
            return Result.error("不支持的文件类型，仅支持: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        // 6. 文件大小验证（双重检查，虽然Spring已配置）
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件上传失败：文件过大 - {} bytes", file.getSize());
            return Result.error("文件大小不能超过50MB");
        }

        // 7. 生成安全的文件名（UUID + 扩展名）
        String objectName = UUID.randomUUID().toString() + extension;

        // 8. 执行上传
        try {
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            log.info("文件上传成功：{} -> {}", originalFilename, filePath);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", originalFilename, e);
            return Result.error(UPLOAD_FAILED);
        }
    }

}
