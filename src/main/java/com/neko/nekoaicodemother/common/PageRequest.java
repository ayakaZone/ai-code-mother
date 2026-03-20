package com.neko.nekoaicodemother.common;

import lombok.Data;

@Data
public class PageRequest {
    /**
     * 分页请求封装类
     */

    /**
     * 当前页
     */
    private int current = 1;

    /**
     * 页大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序(默认降序)
     */
    private String sortOrder = "descend";
}
