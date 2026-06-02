package com.nav.media;

import com.nav.media.dto.FetchFaviconRequest;
import com.nav.media.dto.MediaResponse;
import com.nav.media.dto.SaveFromUrlRequest;
import com.nav.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 媒体接口:图标的上传、从外链保存、抓取站点 favicon,以及按归属读取媒体文件。
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * 上传图片。
     *
     * @param file 上传文件(表单字段名 file)
     * @return 媒体信息
     */
    @PostMapping("/upload")
    public MediaResponse upload(@RequestParam("file") MultipartFile file) {
        // 1. 为当前用户保存上传文件
        return MediaResponse.from(mediaService.upload(SecurityUtils.currentUserId(), file));
    }

    /**
     * 从图片外链下载并保存。
     *
     * @param request 含图片地址
     * @return 媒体信息
     */
    @PostMapping("/from-url")
    public MediaResponse fromUrl(@Valid @RequestBody SaveFromUrlRequest request) {
        // 1. 为当前用户下载并保存外链图片
        return MediaResponse.from(mediaService.saveFromUrl(SecurityUtils.currentUserId(), request.url()));
    }

    /**
     * 抓取站点 favicon 并保存。
     *
     * @param request 含站点地址
     * @return 媒体信息
     */
    @PostMapping("/fetch-favicon")
    public MediaResponse fetchFavicon(@Valid @RequestBody FetchFaviconRequest request) {
        // 1. 为当前用户抓取并保存站点图标
        return MediaResponse.from(mediaService.fetchFavicon(SecurityUtils.currentUserId(), request.url()));
    }

    /**
     * 读取媒体文件内容(校验归属)。
     * 响应加禁嗅探、内容隔离的安全头,杜绝 SVG 等内嵌脚本被执行。
     *
     * @param id 媒体 id
     * @return 文件字节
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        // 1. 取本人媒体内容
        MediaService.LoadedMedia media = mediaService.loadForOwner(SecurityUtils.currentUserId(), id);
        // 2. 带安全头返回:禁 MIME 嗅探、内联展示、内容安全策略禁脚本、按用户私有缓存
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(media.bytes());
    }
}
