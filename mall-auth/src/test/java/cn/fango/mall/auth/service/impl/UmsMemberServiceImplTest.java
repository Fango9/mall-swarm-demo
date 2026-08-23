package cn.fango.mall.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.fango.mall.auth.api.AuthErrorCode;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.UmsMemberMapper;
import cn.fango.mall.mbg.model.UmsMember;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link UmsMemberServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UmsMemberServiceImplTest {

    @Mock
    private UmsMemberMapper umsMemberMapper;

    private PasswordEncoder passwordEncoder;

    private UmsMemberServiceImpl umsMemberService;

    /**
     * 创建待测试的会员服务。
     */
    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        umsMemberService = new UmsMemberServiceImpl(
                umsMemberMapper,
                passwordEncoder
        );
    }

    /**
     * 注册时应写入 BCrypt 哈希、MEMBER 角色与启用状态。
     */
    @Test
    void shouldRegisterMemberWithBcryptPasswordAndDefaultRole() {
        when(umsMemberMapper.selectByExample(any())).thenReturn(List.of());
        when(umsMemberMapper.insertSelective(any(UmsMember.class))).thenAnswer(invocation -> {
            UmsMember member = invocation.getArgument(0);
            member.setId(1L);
            return 1;
        });

        Long memberId = umsMemberService.register("  alice  ", "Passw0rd!");

        ArgumentCaptor<UmsMember> memberCaptor = ArgumentCaptor.forClass(UmsMember.class);
        verify(umsMemberMapper).insertSelective(memberCaptor.capture());

        UmsMember savedMember = memberCaptor.getValue();
        assertThat(memberId).isEqualTo(1L);
        assertThat(savedMember.getUsername()).isEqualTo("alice");
        assertThat(savedMember.getPassword()).isNotEqualTo("Passw0rd!");
        assertThat(passwordEncoder.matches("Passw0rd!", savedMember.getPassword())).isTrue();
        assertThat(savedMember.getRole()).isEqualTo("MEMBER");
        assertThat(savedMember.getStatus()).isEqualTo((byte) 1);
    }

    /**
     * 用户名已存在时应拒绝注册，且不写入新会员。
     */
    @Test
    void shouldRejectRegistrationWhenUsernameAlreadyExists() {
        UmsMember existingMember = new UmsMember();
        existingMember.setId(1L);
        existingMember.setUsername("alice");

        when(umsMemberMapper.selectByExample(any())).thenReturn(List.of(existingMember));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> umsMemberService.register("alice", "Passw0rd!")
        );

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        verify(umsMemberMapper, never()).insertSelective(any(UmsMember.class));
    }
}