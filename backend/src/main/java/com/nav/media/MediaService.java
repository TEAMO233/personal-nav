package com.nav.media;

import com.nav.common.error.ApiException;
import com.nav.media.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * 媒体业务:处理图标的三种来源(上传 / 图片外链 / 站点 favicon),落库并按归属读取。
 */
@Service
public class MediaService {

    /** 允许的图片类型白名单(按文件头探测出的真实类型,含 SVG 但读取时做隔离) */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/x-icon", "image/svg+xml");

    private final StorageService storageService;
    private final MediaAssetRepository mediaAssetRepository;
    private final RemoteContentFetcher fetcher;
    private final FaviconService faviconService;

    public MediaService(StorageService storageService,
                        MediaAssetRepository mediaAssetRepository,
                        RemoteContentFetcher fetcher,
                        FaviconService faviconService) {
        this.storageService = storageService;
        this.mediaAssetRepository = mediaAssetRepository;
        this.fetcher = fetcher;
        this.faviconService = faviconService;
    }

    /**
     * 保存用户上传的图片。
     *
     * @param userId 用户 id
     * @param file   上传文件
     * @return 落库后的媒体
     */
    @Transactional
    public MediaAsset upload(UUID userId, MultipartFile file) {
        // 1. 文件不能为空
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEDIA_FILE_EMPTY", "请选择要上传的文件");
        }
        // 2. 读字节(大小上限由 multipart 配置在更外层拦截)
        byte[] bytes = readBytes(file);
        // 3. 按文件头探测真实类型并校验白名单
        String contentType = requireAllowedImage(bytes);
        // 4. 存盘并落库
        return persist(userId, MediaType.UPLOAD, bytes, contentType, null);
    }

    /**
     * 从图片外链下载并保存。
     *
     * @param userId 用户 id
     * @param url    图片地址
     * @return 落库后的媒体
     */
    @Transactional
    public MediaAsset saveFromUrl(UUID userId, String url) {
        // 1. 下载(SSRF 校验、超时、大小上限都在 fetcher 内)
        RemoteContentFetcher.FetchResult result = fetcher.fetch(url);
        // 2. 探测真实类型并校验白名单
        String contentType = requireAllowedImage(result.bytes());
        // 3. 存盘并落库,记原始外链
        return persist(userId, MediaType.URL, result.bytes(), contentType, url);
    }

    /**
     * 抓取站点 favicon 并保存。
     *
     * @param userId  用户 id
     * @param siteUrl 站点地址
     * @return 落库后的媒体
     */
    @Transactional
    public MediaAsset fetchFavicon(UUID userId, String siteUrl) {
        // 1. 抓站点图标
        RemoteContentFetcher.FetchResult result = faviconService.fetch(siteUrl);
        // 2. 探测真实类型并校验白名单
        String contentType = requireAllowedImage(result.bytes());
        // 3. 存盘并落库,记站点地址
        return persist(userId, MediaType.FAVICON, result.bytes(), contentType, siteUrl);
    }

    /**
     * 按 id 读取属于该用户的媒体,不存在或越权均 404。
     *
     * @param userId 用户 id
     * @param id     媒体 id
     * @return 媒体内容(字节 + 类型)
     */
    @Transactional(readOnly = true)
    public LoadedMedia loadForOwner(UUID userId, UUID id) {
        // 1. 取本人媒体,越权当作不存在(不暴露资源存在性)
        MediaAsset asset = mediaAssetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "媒体不存在"));
        // 2. 从存储读字节
        byte[] bytes = storageService.load(asset.getStorageKey());
        return new LoadedMedia(bytes, asset.getContentType());
    }

    /**
     * 校验某媒体存在且属于该用户,供引擎/快捷方式引用图标前调用;不存在或越权都抛 400。
     * 拦在写入前,避免把非法的 icon_asset_id 写库触发外键异常变成 500。
     *
     * @param userId 用户 id
     * @param id     媒体 id
     */
    @Transactional(readOnly = true)
    public void assertOwned(UUID userId, UUID id) {
        // 1. 查不到本人名下的该媒体,即视为非法引用(不存在或越权都一样)
        if (!mediaAssetRepository.existsByIdAndUserId(id, userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ICON_ASSET_INVALID", "图标不存在或不属于当前用户");
        }
    }

    /**
     * 把字节存盘并建一条媒体记录。
     */
    private MediaAsset persist(UUID userId, MediaType type, byte[] bytes, String contentType, String sourceUrl) {
        // 1. 存到文件存储拿存储键
        String key = storageService.store(bytes, contentType);
        // 2. 建实体并落库
        MediaAsset asset = new MediaAsset();
        asset.setUserId(userId);
        asset.setType(type);
        asset.setStorageKey(key);
        asset.setContentType(contentType);
        asset.setSourceUrl(sourceUrl);
        return mediaAssetRepository.save(asset);
    }

    /**
     * 按文件头探测真实图片类型,不在白名单内则拒。
     */
    private String requireAllowedImage(byte[] bytes) {
        // 1. 探测真实类型
        String detected = ImageTypeDetector.detect(bytes);
        // 2. 认不出或不在白名单都拒(挡住伪装成图片的内容)
        if (detected == null || !ALLOWED_TYPES.contains(detected)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MEDIA_UNSUPPORTED_TYPE", "不支持的图片类型");
        }
        return detected;
    }

    /**
     * 读取上传文件字节。
     */
    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEDIA_FILE_READ_FAILED", "读取上传文件失败");
        }
    }

    /**
     * 读取到的媒体内容。
     *
     * @param bytes       文件字节
     * @param contentType 文件 MIME 类型
     */
    public record LoadedMedia(byte[] bytes, String contentType) {
    }
}
