package com.neko.nekoaicodemother.controller;

import com.neko.nekoaicodemother.common.BaseResponse;
import com.neko.nekoaicodemother.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@Tag(name = "健康检查接口")
public class HealthController {

    /**
     * 健康检查接口
     * @return 响应
     */
    @GetMapping("/")
    @Operation(summary = "健康检查接口")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
