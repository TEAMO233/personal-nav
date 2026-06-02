package com.nav.media;

import java.nio.charset.StandardCharsets;

/**
 * 图片类型探测:只认文件头魔数,不信任客户端声称的类型,挡住伪装成图片的可执行内容。
 */
public final class ImageTypeDetector {

    private ImageTypeDetector() {
    }

    /**
     * 按文件头魔数探测图片 MIME 类型。
     *
     * @param bytes 文件字节
     * @return 识别到的图片 MIME 类型;无法识别返回 null
     */
    public static String detect(byte[] bytes) {
        // 1. 太短直接判定不是图片
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        // 2. PNG:89 50 4E 47
        if (match(bytes, 0, 0x89, 0x50, 0x4E, 0x47)) {
            return "image/png";
        }
        // 3. JPEG:FF D8 FF
        if (match(bytes, 0, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        // 4. GIF:47 49 46 38(GIF8)
        if (match(bytes, 0, 0x47, 0x49, 0x46, 0x38)) {
            return "image/gif";
        }
        // 5. WEBP:开头 RIFF,第 8 字节起 WEBP
        if (match(bytes, 0, 0x52, 0x49, 0x46, 0x46) && match(bytes, 8, 0x57, 0x45, 0x42, 0x50)) {
            return "image/webp";
        }
        // 6. ICO:00 00 01 00
        if (match(bytes, 0, 0x00, 0x00, 0x01, 0x00)) {
            return "image/x-icon";
        }
        // 7. SVG:文本型,单独判断开头标签
        if (looksLikeSvg(bytes)) {
            return "image/svg+xml";
        }
        // 8. 都不匹配
        return null;
    }

    /**
     * 比较从 offset 起的字节是否与给定序列一致。
     */
    private static boolean match(byte[] bytes, int offset, int... expected) {
        // 1. 越界直接不匹配
        if (offset + expected.length > bytes.length) {
            return false;
        }
        // 2. 逐字节比较
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断文本内容是否像 SVG(开头是 &lt;svg、&lt;!doctype svg,或 &lt;?xml 且后面含 &lt;svg)。
     */
    private static boolean looksLikeSvg(byte[] bytes) {
        // 1. 跳过可能的 UTF-8 BOM
        int start = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            start = 3;
        }
        // 2. 取开头一小段转小写后去空白
        int limit = Math.min(bytes.length, 1024);
        String head = new String(bytes, start, limit - start, StandardCharsets.UTF_8).trim().toLowerCase();
        // 3. 直接以 svg 标签开头
        if (head.startsWith("<svg") || head.startsWith("<!doctype svg")) {
            return true;
        }
        // 4. 以 xml 声明开头时,需后面确实出现 <svg
        return head.startsWith("<?xml") && head.contains("<svg");
    }
}
