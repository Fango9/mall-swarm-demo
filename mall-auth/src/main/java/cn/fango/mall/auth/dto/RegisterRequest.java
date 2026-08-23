package cn.fango.mall.auth.dto;

/**
 * 会员注册请求。
 */
public class RegisterRequest {

    private String username;

    private String password;

    /**
     * 获取登录用户名。
     *
     * @return 登录用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置登录用户名。
     *
     * @param username 登录用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取明文密码。
     *
     * @return 明文密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置明文密码。
     *
     * @param password 明文密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

}