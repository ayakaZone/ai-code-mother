package com.neko.nekoaicodemother.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.neko.nekoaicodemother.langgraph4j.model.ImageCategoryEnum;
import com.neko.nekoaicodemother.langgraph4j.model.ImageResource;
import com.neko.nekoaicodemother.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LogoGeneratorTool {

    @Resource
    private CosManager cosManager;

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
    private String imageModel;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // 构建 Logo 设计提示词
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .prompt(logoPrompt)
                    .size("512*512")
                    .n(1) // 生成 1 张足够，因为 AI 不知道哪张最好
                    .build();
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    String imageUrl = imageResult.get("url");
                    if (StrUtil.isNotBlank(imageUrl)) {
                        // 下载图片
                        File file = downloadImageFileToLocal(imageUrl);
                        // 上传图片到 COS
                        String keyName = String.format("/logo/%s/%s", RandomUtil.randomString(5), file.getName());
                        String cosUrl = cosManager.uploadFile(keyName, file);
                        // 删除本地临时图片文件
                        deleteLocalImageFile(file.getPath());
                        // 构造图片类作为列表返回结果
                        logoList.add(ImageResource.builder()
                                .category(ImageCategoryEnum.LOGO)
                                .description(description)
                                .url(cosUrl)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }

    /**
     * 下载 OSS 图片文件
     *
     * @param url 图片 URL
     * @return 图片文件
     */
    private File downloadImageFileToLocal(String url) {
        // 构造本地临时目录
        String logoRootDir = System.getProperty("user.dir") + "/tmp/logo_image";
        Path path = Paths.get(logoRootDir);
        // 判断目录是否存在
        if (!FileUtil.isDirectory(path)) {
            FileUtil.mkdir(path);
        }
        // 构造本地临时文件名
        String fileName = RandomUtil.randomString(5) + ".jpg";
        File file = new File(logoRootDir + File.separator + fileName);
        // 通过 Hutool 工具类下载图片到本地临时目录
        long fileSize = HttpUtil.downloadFile(url, file);
        if (fileSize < 0) {
            log.error("下载Logo图片失败: {} -> {}", url, file.getPath());
        }
        return file;
    }

    /**
     * 删除本地临时图片文件
     * @param url 图片 URL
     */
    private void deleteLocalImageFile(String url) {
        // 判断文件是否存在
        Path path = Paths.get(url);
        if (!Files.exists(path)) {
            log.error("本地临时logo图片文件不存在: {}", url);
            return;
        }
        // 判断是不是一个文件
        if (!Files.isRegularFile(path)) {
            log.error("本地临时logo图片不是一个文件: {}", url);
            return;
        }
        // 删除本地临时文件
        try {
            Files.delete(path);
        } catch (IOException e) {
            log.error("删除本地临时logo图片失败: {}", url, e);
        }
    }
}
