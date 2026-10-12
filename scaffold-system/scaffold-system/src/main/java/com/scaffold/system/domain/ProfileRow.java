package com.scaffold.system.domain;

/** 当前账号的个人资料行，时间字段已在 SQL 中格式化。 */
public class ProfileRow {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String deptName;
    private String lastLoginTime;
    private String createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(String lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
