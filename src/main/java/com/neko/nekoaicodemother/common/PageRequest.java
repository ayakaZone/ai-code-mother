package com.neko.nekoaicodemother.common;

import lombok.Data;

/**
 * 分页请求封装类
 */
@Data
public class PageRequest {

    /**
     * 当前页
     */
    private long pageNum = 1;

    /**
     * 页大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序(默认降序)
     */
    private String sortOrder = "descend";
}
