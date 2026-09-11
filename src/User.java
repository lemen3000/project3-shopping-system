import java.time.LocalDateTime;

public abstract class User implements UserOperation {
    private int id;
    private String name;
    private String password;
    private String phone;
    private String email;
    private LocalDateTime registerTime;
    private int errorCount;
    private boolean isLock;

    public User(int id, String name, String password, String phone, String email, LocalDateTime registerTime, int errorCount, boolean isLock) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.phone = phone;
        this.email = email;
        this.registerTime = registerTime;
        this.errorCount = errorCount;
        this.isLock = isLock;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getRegisterTime() { return registerTime; }
    public void setRegisterTime(LocalDateTime registerTime) { this.registerTime = registerTime; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public boolean isLock() { return isLock; }
    public void setLock(boolean lock) { isLock = lock; }

    @Override
    public abstract boolean login(String name, String password);
    @Override
    public abstract void logout();
    @Override
    public abstract boolean modifyPassword(String oldPassword, String newPassword);

    public abstract void showInfo();

    // 重置随机密码
    public String resetRandomPwd() {
        return "123456";
    }

    // 密码规则校验
    public boolean checkPasswordRule(String password) {
        return password.length() >= 6;
    }

    // 用户名规则校验
    public boolean checkUserNameRule(String username) {
        return username.length() > 0;
    }

    // 忘记密码验证用户名+邮箱
    public boolean verifyUserAndEmail(String uname, String mail) {
        return uname.equals(this.name) && mail.equals(this.email);
    }
}