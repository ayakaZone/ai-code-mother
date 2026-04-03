package com.neko.nekoaicodemother.service;

public interface ScreenshotService {

    /**
     * 生成网页截图并上传到 COS
     * @param webUrl 网页链接
     * @return COS 图片访问路径
     */
    String generateAndUploadScreenshot(String webUrl);
}
