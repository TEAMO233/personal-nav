package com.nav.media.storage;

import com.nav.common.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 本地磁盘文件存储:把文件存到配置目录下,按「年/月/随机uuid」组织,返回相对路径作为存储键。
 */
@Service
public class LocalDiskStorageService implements StorageService {

    /** 存储根目录(绝对路径) */
    private final Path baseDir;

    public LocalDiskStorageService(@Value("${app.storage.local.base-dir:./data/media}") String baseDir) {
        // 1. 记住存储根目录的绝对、规范化路径
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content, String contentType) {
        // 1. 按当前年月拼相对目录,文件名用随机 uuid(防重名、防枚举)
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String key = "%04d/%02d/%s".formatted(now.getYear(), now.getMonthValue(), UUID.randomUUID());
        Path target = resolve(key);
        try {
            // 2. 建好父目录再写文件
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_STORE_FAILED", "保存文件失败");
        }
        // 3. 返回相对存储键
        return key;
    }

    @Override
    public byte[] load(String key) {
        // 1. 定位文件,不存在按未找到处理
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "文件不存在");
        }
        try {
            // 2. 读出全部字节
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_READ_FAILED", "读取文件失败");
        }
    }

    @Override
    public void delete(String key) {
        try {
            // 1. 删除文件,不存在忽略
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_DELETE_FAILED", "删除文件失败");
        }
    }

    @Override
    public boolean exists(String key) {
        // 1. 判断文件是否存在
        return Files.exists(resolve(key));
    }

    /**
     * 把存储键解析为根目录下的绝对路径,并拦截越界路径(防 ../ 目录穿越)。
     */
    private Path resolve(String key) {
        // 1. 拼到根目录并规范化
        Path target = baseDir.resolve(key).normalize();
        // 2. 必须仍在根目录内,否则视为非法键
        if (!target.startsWith(baseDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEDIA_KEY_INVALID", "非法存储键");
        }
        return target;
    }
}
