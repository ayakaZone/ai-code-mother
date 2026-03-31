package com.neko.nekoaicodemother.contorller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.neko.nekoaicodemother.annotation.AuthCheck;
import com.neko.nekoaicodemother.common.BaseResponse;
import com.neko.nekoaicodemother.common.DeleteRequest;
import com.neko.nekoaicodemother.common.ResultUtils;
import com.neko.nekoaicodemother.constant.UserConstant;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import com.neko.nekoaicodemother.model.dto.user.*;
import com.neko.nekoaicodemother.model.vo.user.LoginUserVO;
import com.neko.nekoaicodemother.model.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.service.UserService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户控制层。
 *
 * @author mosoNeko
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
public class UserController {

    @Resource
    private UserService userService;


    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 用户id
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @return 脱敏登录用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 脱敏登录用户信息
     */
    @GetMapping("/get/login")
    @Operation(summary = "用户获取当前登录用户")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 是否注销成功
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userLogout(request));
    }

    /**
     * 创建用户
     *
     * @param userAddRequest 创建用户请求
     * @return 新创建的用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员创建用户")
    public BaseResponse<Long> add(@RequestBody UserAddRequest userAddRequest) {
        // 校验参数
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        // 拷贝对象
        BeanUtil.copyProperties(userAddRequest, user);
        // 初始化默认密码并加密
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        // 添加用户
        boolean saveResult = userService.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据Id获取用户信息
     *
     * @param id 用户Id
     * @return 用户信息
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员根据Id获取用户")
    public BaseResponse<User> getUserById(long id) {
        // 校验参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取用户
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据Id获取脱敏用户信息
     *
     * @param id 用户Id
     * @return 脱敏用户信息
     */
    @PostMapping("/get/vo")
    @Operation(summary = "用户根据Id获取脱敏用户")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // 校验参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取用户并脱敏
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     * @param deleteRequest 删除用户请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员删除用户")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 校验参数
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean deleteResult = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!deleteResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(deleteResult);
    }

    /**
     * 更新用户
     * @param userUpdateRequest 更新用户请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员更新用户")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 校验参数
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean updateResult = userService.updateById(user);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(updateResult);
    }

    /**
     * 获取脱敏用户列表
     * @param userQueryRequest 用户查询请求
     * @return 用户列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员获取脱敏用户列表")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 校验参数
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取分页参数
        long pageSize = userQueryRequest.getPageSize();
        long pageNum = userQueryRequest.getPageNum();
        // 分页查询用户信息
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize), userService.getQueryWrapper(userQueryRequest));
        // 脱敏
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        // 封装Page
        Page<UserVO> userVoPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        userVoPage.setRecords(userVOList);
        return ResultUtils.success(userVoPage);
    }
}
