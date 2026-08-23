package cn.fango.mall.admin.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.fango.mall.mbg.mapper.UmsMemberMapper;
import cn.fango.mall.mbg.model.UmsMember;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 为 Sa-Token 提供后台账号角色信息。
 */
@Component
public class AdminStpInterface implements StpInterface {

    private static final byte ENABLED_STATUS = 1;

    private final UmsMemberMapper umsMemberMapper;

    /**
     * 创建后台角色提供者。
     *
     * @param umsMemberMapper 会员数据访问对象
     */
    public AdminStpInterface(UmsMemberMapper umsMemberMapper) {
        this.umsMemberMapper = umsMemberMapper;
    }

    /**
     * 获取账号拥有的权限标识。
     *
     * <p>当前流程仅使用角色校验，暂未实现菜单权限体系。</p>
     *
     * @param loginId 当前登录账号主键
     * @param loginType Sa-Token 登录类型
     * @return 空权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 获取账号拥有的角色标识。
     *
     * @param loginId 当前登录账号主键
     * @param loginType Sa-Token 登录类型
     * @return 当前启用账号的单个角色；查询不到时返回空列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long memberId = parseMemberId(loginId);
        if (memberId == null) {
            return Collections.emptyList();
        }

        UmsMember member = umsMemberMapper.selectByPrimaryKey(memberId);
        if (member == null
                || member.getStatus() == null
                || member.getStatus() != ENABLED_STATUS
                || !StringUtils.hasText(member.getRole())) {
            return Collections.emptyList();
        }

        return List.of(member.getRole());
    }

    /**
     * 将 Sa-Token 登录标识转换为会员主键。
     *
     * @param loginId Sa-Token 登录标识
     * @return 会员主键；无法转换时返回 {@code null}
     */
    private Long parseMemberId(Object loginId) {
        if (loginId == null) {
            return null;
        }

        try {
            return Long.valueOf(loginId.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}