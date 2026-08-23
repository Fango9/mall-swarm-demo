package cn.fango.mall.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.fango.mall.auth.api.AuthErrorCode;
import cn.fango.mall.auth.dto.LoginRequest;
import cn.fango.mall.auth.dto.LoginResponse;
import cn.fango.mall.auth.dto.MemberProfileResponse;
import cn.fango.mall.auth.dto.RegisterRequest;
import cn.fango.mall.auth.service.UmsMemberService;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.model.UmsMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String TOKEN_TYPE = "Bearer";

    private final UmsMemberService umsMemberService;

    /**
     * 创建认证接口。
     *
     * @param umsMemberService 会员身份服务
     */
    public AuthController(UmsMemberService umsMemberService) {
        this.umsMemberService = umsMemberService;
    }

    /**
     * 注册普通会员。
     *
     * @param request 注册请求
     * @return 新会员主键
     */
    @PostMapping("/register")
    public CommonResult<Long> register(@RequestBody(required = false) RegisterRequest request) {
        String username = request == null ? null : request.getUsername();
        String password = request == null ? null : request.getPassword();

        Long memberId = umsMemberService.register(username, password);
        return CommonResult.success(memberId, "注册成功");
    }

    /**
     * 登录并创建 Redis 会话。
     *
     * @param request 登录请求
     * @return 登录令牌与当前身份信息
     */
    @PostMapping("/login")
    public CommonResult<LoginResponse> login(@RequestBody(required = false) LoginRequest request) {
        String username = request == null ? null : request.getUsername();
        String password = request == null ? null : request.getPassword();

        UmsMember member = umsMemberService.authenticate(username, password);
        StpUtil.login(member.getId());

        LoginResponse response = new LoginResponse(
                member.getId(),
                member.getUsername(),
                member.getRole(),
                StpUtil.getTokenValue(),
                TOKEN_TYPE
        );
        return CommonResult.success(response, "登录成功");
    }

    /**
     * 查询当前登录会员的身份信息。
     *
     * @return 当前登录会员的身份信息
     */
    @GetMapping("/me")
    public CommonResult<MemberProfileResponse> me() {
        StpUtil.checkLogin();

        Long memberId = StpUtil.getLoginIdAsLong();
        UmsMember member = umsMemberService.findById(memberId);
        if (member == null) {
            StpUtil.logout();
            throw new ApiException(AuthErrorCode.MEMBER_NOT_FOUND);
        }

        MemberProfileResponse response = new MemberProfileResponse(
                member.getId(),
                member.getUsername(),
                member.getRole()
        );
        return CommonResult.success(response);
    }

    /**
     * 退出当前登录会话。
     *
     * @return 退出结果
     */
    @PostMapping("/logout")
    public CommonResult<Void> logout() {
        StpUtil.logout();
        return CommonResult.success(null, "退出成功");
    }
}