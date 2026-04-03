package com.neko.nekoaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import com.neko.nekoaicodemother.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 需要过滤的文件拓展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     * 检查文件路径是否允许下载
     *
     * @param projectRoot 项目根目录
     * @param fullPath    文件全路径
     * @return 是否允许下载
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 获取文件名称
        Path relativePath = projectRoot.relativize(fullPath);
        for (Path part : relativePath) {
            String partName = part.toString();
            // 检查是否在忽略名单内
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            // 检查拓展名
            if (IGNORED_EXTENSIONS.contains(partName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 项目打包下载
     * @param projectPath 项目路径
     * @param downloadFileName 下载文件名
     * @param response 响应
     */
    @Override
    public void downLoadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR, "项目路径不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR, "下载文件名不能为空");
        // 检查项目是否存在
        File projectFile = new File(projectPath);
        ThrowUtils.throwIf(!projectFile.exists(), ErrorCode.NOT_FOUND_ERROR, "项目不存在");
        ThrowUtils.throwIf(!projectFile.isDirectory(), ErrorCode.PARAMS_ERROR, "项目路径必须是目录");
        log.info("开始打包下载项目：{} -> {}.zip", projectPath, downloadFileName);
        // HTTP 响应头，代表这是下载文件响应
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", downloadFileName));
        // 定义文件过滤器
        FileFilter fileFilter = file -> isPathAllowed(projectFile.toPath(), file.toPath());
        // 压缩 Zip 文件写入 HTTP 响应流
        try {
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, fileFilter, projectFile);
            log.info("项目打包完成，已下载：{} -> {}.zip", projectPath, downloadFileName);
        } catch (IOException e) {
            log.error("项目打包失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目打包失败");
        }
    }
}
