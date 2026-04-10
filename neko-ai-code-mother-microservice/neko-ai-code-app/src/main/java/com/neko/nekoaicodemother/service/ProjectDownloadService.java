package com.neko.nekoaicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {

    /**
     * 项目打包下载
     * @param projectPath 项目路径
     * @param downloadFileName 下载文件名
     * @param response 响应
     */
    void downLoadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
