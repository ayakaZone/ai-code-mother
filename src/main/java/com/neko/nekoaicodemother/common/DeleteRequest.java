package com.neko.nekoaicodemother.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    /**
     * 删除请求封装类
     */

    private static final long serialVersionUID = 1L;

    /**
     *  id
     */
    private Long id;
}
