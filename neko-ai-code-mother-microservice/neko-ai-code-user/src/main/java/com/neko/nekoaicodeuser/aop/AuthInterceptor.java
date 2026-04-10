package com.neko.nekoaicodeuser.aop;

import com.neko.nekoaicodemother.annotation.AuthCheck;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.enums.UserRoleEnum;
import com.neko.nekoaicodeuser.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 权限校验拦截逻辑
     *
     * @param joinPoint 切点
     * @param authCheck 注解
     * @return 拦截结果
     * @throws Throwable 抛出异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取必要权限
        String mustRole = authCheck.mustRole();
        // 必要权限对应枚举类
        UserRoleEnum enumMustRole = UserRoleEnum.getEnumByValue(mustRole);
        // 获取当前用户
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        // 不需要权限
        if (enumMustRole == null) {
            return joinPoint.proceed();
        }
        // 需要用户权限
        // 判断当前用户的权限
        UserRoleEnum enumUserRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (enumUserRole == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 判断必要权限是管理员且用户权限不是管理员
        if (enumMustRole.equals(UserRoleEnum.ADMIN) && !enumUserRole.equals(UserRoleEnum.ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 必要权限为用户且用户有权限——通行
        return joinPoint.proceed();
    }
}
