package com.neko.nekoaicodemother.langgraph4j.ai;


import com.neko.nekoaicodemother.langgraph4j.tools.ImageSearchTool;
import com.neko.nekoaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.neko.nekoaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.neko.nekoaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    /**
     * 创建图片收集服务
     * @return 图片收集服务
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(imageSearchTool, logoGeneratorTool, mermaidDiagramTool, undrawIllustrationTool)
                .build();
    }
}
