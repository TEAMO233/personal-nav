package com.nav.media.storage;

/**
 * 文件存储抽象:屏蔽本地磁盘 / 对象存储的差异,业务层只认存储键。
 * 日后要换 MinIO、OSS,只需另写一个实现并切换配置,不动业务代码。
 */
public interface StorageService {

    /**
     * 保存一段二进制内容,返回用于定位的存储键。
     *
     * @param content     文件字节
     * @param contentType 文件 MIME 类型
     * @return 存储键(相对路径)
     */
    String store(byte[] content, String contentType);

    /**
     * 按存储键读取文件全部字节(图标都很小,直接整块读)。
     *
     * @param key 存储键
     * @return 文件字节
     */
    byte[] load(String key);

    /**
     * 删除指定存储键的文件,文件不存在则忽略。
     *
     * @param key 存储键
     */
    void delete(String key);

    /**
     * 判断存储键对应的文件是否存在。
     *
     * @param key 存储键
     * @return 存在返回 true
     */
    boolean exists(String key);
}
