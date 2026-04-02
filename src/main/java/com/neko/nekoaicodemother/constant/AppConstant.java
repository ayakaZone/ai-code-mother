package com.neko.nekoaicodemother.constant;

public interface AppConstant {

    /**
     * 精选的app优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认的app优先级
     */
    Integer BAD_APP_PRIORITY = 0;

    /**
     * 应用生成目录(预览)
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署地址（域名）
     */
    String CODE_DEPLOY_HOST = "http://localhost";
}
