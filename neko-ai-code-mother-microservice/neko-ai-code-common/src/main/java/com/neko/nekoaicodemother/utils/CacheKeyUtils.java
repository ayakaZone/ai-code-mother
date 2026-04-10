package com.neko.nekoaicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

public class CacheKeyUtils {

    /**
     * 将对象转为 RedisCacheKey
     * @param obj 对象
     * @return RedisCacheKey
     */
    public static String generateKey(Object obj){
        // 处理 null 对象情况
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 将对象转为 JSON 字符串
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}
