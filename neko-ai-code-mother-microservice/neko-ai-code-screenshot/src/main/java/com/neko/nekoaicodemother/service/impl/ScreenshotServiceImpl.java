package com.neko.nekoaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import com.neko.nekoaicodemother.manager.CosManager;
import com.neko.nekoaicodemother.service.ScreenshotService;
import com.neko.nekoaicodemother.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    /**
     * 生成网页截图并上传到 COS
     * @param webUrl 网页链接
     * @return COS 图片访问路径
     */
    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页链接不能为空");
        log.info("开始生成网页截图：{}", webUrl);
        // 获取网页截图
        String screenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(screenshotPath), ErrorCode.SYSTEM_ERROR, "生成网页截图失败");
        log.info("网页截图生成成功，开始上传到 COS：{}", screenshotPath);
        try {
            // 上传图片到 COS
            String cosUrl = uploadScreenshotToCos(screenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.SYSTEM_ERROR, "上传图片到 COS 失败");
            log.info("网页截图上传成功，COS 图片访问路径：{}", cosUrl);
            return cosUrl;
        } finally {
            // 清除本地临时截图文件
            clearUploadLocalFile(screenshotPath);
        }
    }

    private String uploadScreenshotToCos(String screenshotPath) {
        // 判断截图文件是否存在
        File screenshotFile = new File(screenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在：{}", screenshotPath);
            return null;
        }
        // 构建 COS 对象键
        String key = generateScreenshotKey();
        // 上传文件到 COS
        return cosManager.uploadFile(key, screenshotFile);
    }

    /**
     * 生成上传 COS 对象键
     * @return COS 对象键
     */
    private String generateScreenshotKey() {
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String dataPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshot/%s/%s", dataPath, fileName);
    }

    /**
     * 清理上传的临时文件
     * @param screenshotPath 截图文件路径
     */
    private void clearUploadLocalFile(String screenshotPath) {
        File screenshotFile = new File(screenshotPath);
        if (screenshotFile.exists()) {
            File parentDir = screenshotFile.getParentFile();
            parentDir.delete();
            log.info("本地截图文件已清理：{}", screenshotPath);
        }
    }
}
