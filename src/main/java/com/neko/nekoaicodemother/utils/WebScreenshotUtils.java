package com.neko.nekoaicodemother.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 网页截图工具类
 */
@Slf4j
public class WebScreenshotUtils {

    // 配置 WebDriver 驱动

    private final static WebDriver webDriver;

    // 通过静态代码块初始化，无需重复初始化
    static {
        // 打开网页的页面大小 16:9
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        // 初始化 ChromeDriver
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * 生成并保存网页截图
     *
     * @param webUrl 网页链接
     * @return 压缩图片路径
     */
    public static String saveWebPageScreenshot(String webUrl) {
        // 参数校验
        ThrowUtils.throwIf(webUrl == null, ErrorCode.PARAMS_ERROR, "网页链接不能为空");
        try {
            // 访问网页（打开网页）
            webDriver.get(webUrl);
            // 等待网页加载完毕
            waitForPageLoad();
            // 截图并保存为字节数组
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // 图片根目录
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator
                    + "screenshot" + File.separator + UUID.randomUUID().toString().substring(0, 8);
            // 构建根目录
            FileUtil.mkdir(rootPath);
            // 保存图片和压缩图片的后缀
            final String IMAGE_SUFFIX = ".png";
            final String IMAGE_COMPRESS_SUFFIX = "_compress.jpg";
            // 构建保存图片和压缩图片的根路径
            String imageRootPath = rootPath + File.separator + UUID.randomUUID().toString().substring(0, 5) + IMAGE_SUFFIX;
            String imageCompressPath = rootPath + File.separator + UUID.randomUUID().toString().substring(0, 5) + IMAGE_COMPRESS_SUFFIX;
            // 保存图片到本地
            saveImage(screenshotBytes, imageRootPath);
            log.info("原始图片截图成功，保存路径：{}", imageRootPath);
            // 压缩图片质量
            compressImage(imageRootPath, imageCompressPath);
            log.info("图片压缩成功，保存路径：{}", imageCompressPath);
            // 删除原始图片
            FileUtil.del(imageRootPath);
            return imageCompressPath;
        } catch (Exception e) {
            log.error("生成网页截图失败：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 保存图片
     *
     * @param imageBytes 图片字节数组
     * @param imagePath  图片保存路径
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            // 保存图片
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (IORuntimeException e) {
            log.error("保存图片失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片
     *
     * @param originalImagePath   原图片路径
     * @param compressedImagePath 压缩图片保存路径
     */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        // 压缩完成后的图片质量
        final float QUALITY = 0.3f;
        try {
            // 压缩图片并保存到指定路径
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    QUALITY
            );
        } catch (IORuntimeException e) {
            log.error("压缩图片失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 等待页面加载完成
     */
    private static void waitForPageLoad() {
        // 配置页面加载时间 30 秒
        WebDriverWait wait = new WebDriverWait(WebScreenshotUtils.webDriver, Duration.ofSeconds(10));
        // 等待直到页面加载完毕
        wait.until(webDriver -> Objects.equals(
                ((JavascriptExecutor) webDriver).executeScript("return document.readyState"), "complete")
        );
        // 休眠 2 秒确保页面渲染完毕
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error("等待页面加载完成时发生异常：{}", e.getMessage(), e);
        }
    }

    /**
     * 初始化 ChromeDriver
     *
     * @param width  宽度
     * @param height 高度
     * @return WebDriver
     */
    private static WebDriver initChromeDriver(int width, int height) {

        try {
            // WebDriverManager 安装 Chrome 浏览器驱动并自动管理
            WebDriverManager.chromedriver().setup();
            // 获取 WebDriver
            WebDriver driver = getWebDriver(width, height);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 获取 WebDriver
     *
     * @param width  宽度
     * @param height 高度
     * @return WebDriver
     */
    private static WebDriver getWebDriver(int width, int height) {
        // 配置 Chrome 选项
        ChromeOptions options = new ChromeOptions();
        // 无头模式
        options.addArguments("--headless");
        // 禁用GPU（在某些环境下避免问题）
        options.addArguments("--disable-gpu");
        // 禁用沙盒模式（Docker环境需要）
        options.addArguments("--no-sandbox");
        // 禁用开发者shm使用
        options.addArguments("--disable-dev-shm-usage");
        // 设置窗口大小
        options.addArguments(String.format("--window-size=%d,%d", width, height));
        // 禁用扩展
        options.addArguments("--disable-extensions");
        // 设置用户代理
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        // 创建驱动
        return new ChromeDriver(options);
    }

    /**
     * 优雅停机，保证项目结束后释放驱动
     */
    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }
}
