package cn.fango.mall.auth.service.impl;

import cn.fango.mall.auth.api.AuthErrorCode;
import cn.fango.mall.auth.service.UmsMemberService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.UmsMemberMapper;
import cn.fango.mall.mbg.model.UmsMember;
import cn.fango.mall.mbg.model.UmsMemberExample;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 会员身份服务实现。
 */
@Service
public class UmsMemberServiceImpl implements UmsMemberService {

    private static final String MEMBER_ROLE = "MEMBER";

    private static final byte ENABLED_STATUS = 1;

    private final UmsMemberMapper umsMemberMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建会员身份服务。
     *
     * @param umsMemberMapper 会员数据访问对象
     * @param passwordEncoder 密码编码器
     */
    public UmsMemberServiceImpl(
            UmsMemberMapper umsMemberMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.umsMemberMapper = umsMemberMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String rawPassword) {
        if (!StringUtils.hasText(username)) {
            throw new ApiException(AuthErrorCode.USERNAME_REQUIRED);
        }
        if (!StringUtils.hasText(rawPassword)) {
            throw new ApiException(AuthErrorCode.PASSWORD_REQUIRED);
        }

        String normalizedUsername = username.trim();
        ensureUsernameNotExists(normalizedUsername);

        UmsMember member = new UmsMember();
        member.setUsername(normalizedUsername);
        member.setPassword(passwordEncoder.encode(rawPassword));
        member.setRole(MEMBER_ROLE);
        member.setStatus(ENABLED_STATUS);

        int affectedRows = umsMemberMapper.insertSelective(member);
        if (affectedRows != 1 || member.getId() == null) {
            throw new ApiException(AuthErrorCode.MEMBER_CREATE_FAILED);
        }
        return member.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UmsMember authenticate(String username, String rawPassword) {
        if (!StringUtils.hasText(username)) {
            throw new ApiException(AuthErrorCode.USERNAME_REQUIRED);
        }
        if (!StringUtils.hasText(rawPassword)) {
            throw new ApiException(AuthErrorCode.PASSWORD_REQUIRED);
        }

        String normalizedUsername = username.trim();
        UmsMember member = findMemberByUsername(normalizedUsername);

        if (member == null || !passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new ApiException(AuthErrorCode.LOGIN_FAILED);
        }
        if (member.getStatus() == null || member.getStatus() != ENABLED_STATUS) {
            throw new ApiException(AuthErrorCode.MEMBER_DISABLED);
        }
        return member;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UmsMember findById(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return umsMemberMapper.selectByPrimaryKey(memberId);
    }

    /**
     * 确认用户名尚未被注册。
     *
     * @param username 规范化后的用户名
     */
    private void ensureUsernameNotExists(String username) {
        UmsMember member = findMemberByUsername(username);
        if (member != null) {
            throw new ApiException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }

    /**
     * 按用户名查询会员。
     *
     * @param username 规范化后的用户名
     * @return 查询到的会员；不存在时返回 {@code null}
     */
    private UmsMember findMemberByUsername(String username) {
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andUsernameEqualTo(username);

        List<UmsMember> members = umsMemberMapper.selectByExample(example);
        if (members.isEmpty()) {
            return null;
        }
        return members.get(0);
    }
}