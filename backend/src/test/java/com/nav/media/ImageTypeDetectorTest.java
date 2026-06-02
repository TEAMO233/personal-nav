package com.nav.media;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 图片类型探测单元测试:验证按文件头魔数识别各类图片,并拒绝伪装/过短内容。
 * 只认文件头、不信任客户端声称类型,是挡住伪装成图片的可执行内容的安全关口。
 */
class ImageTypeDetectorTest {

    /**
     * 各类二进制图片魔数被正确识别为对应 MIME 类型。
     */
    @Test
    void detectsImageMagicNumbers() {
        // 1. PNG 签名
        assertThat(ImageTypeDetector.detect(pad(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))).isEqualTo("image/png");
        // 2. JPEG
        assertThat(ImageTypeDetector.detect(pad(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo("image/jpeg");
        // 3. GIF89a
        assertThat(ImageTypeDetector.detect(pad(0x47, 0x49, 0x46, 0x38, 0x39, 0x61))).isEqualTo("image/gif");
        // 4. WEBP:开头 RIFF,第 8 字节起 WEBP
        assertThat(ImageTypeDetector.detect(new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50}))
                .isEqualTo("image/webp");
        // 5. ICO
        assertThat(ImageTypeDetector.detect(pad(0x00, 0x00, 0x01, 0x00))).isEqualTo("image/x-icon");
    }

    /**
     * SVG 文本(直接标签 / xml 声明后含 svg / 带 UTF-8 BOM)被识别为 svg。
     */
    @Test
    void detectsSvgText() {
        // 1. 直接以 <svg 开头
        assertThat(ImageTypeDetector.detect("<svg width=\"1\" height=\"1\"></svg>".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("image/svg+xml");
        // 2. xml 声明后含 <svg
        assertThat(ImageTypeDetector.detect("<?xml version=\"1.0\"?><svg></svg>".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("image/svg+xml");
        // 3. 带 UTF-8 BOM 前缀
        byte[] svg = "<svg></svg> padding".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[svg.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(svg, 0, withBom, 3, svg.length);
        assertThat(ImageTypeDetector.detect(withBom)).isEqualTo("image/svg+xml");
    }

    /**
     * 非图片内容、过短内容、null 一律返回 null(挡住伪装)。
     */
    @Test
    void rejectsNonImage() {
        // 1. 纯文本
        assertThat(ImageTypeDetector.detect("this is just plain text, not an image".getBytes(StandardCharsets.UTF_8)))
                .isNull();
        // 2. 过短(不足 12 字节)
        assertThat(ImageTypeDetector.detect(new byte[]{0x00, 0x01})).isNull();
        // 3. null
        assertThat(ImageTypeDetector.detect(null)).isNull();
    }

    /**
     * 把若干魔数字节填充到至少 16 字节,满足探测要求的最小长度。
     */
    private static byte[] pad(int... head) {
        byte[] bytes = new byte[Math.max(16, head.length)];
        for (int i = 0; i < head.length; i++) {
            bytes[i] = (byte) head[i];
        }
        return bytes;
    }
}
