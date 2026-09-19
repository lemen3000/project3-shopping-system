import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Random;

public abstract class User implements UserOperation, Serializable {
    private static final long serialVersionUID = 1L;

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

    // 生成符合规则的随机密码
    public String resetRandomPwd() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String num = "0123456789";
        String symbol = "!@#$%&*()_+-=.";
        String all = upper + lower + num + symbol;
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(num.charAt(random.nextInt(num.length())));
        sb.append(symbol.charAt(random.nextInt(symbol.length())));
        for (int i = 0; i < 5; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        return sb.toString();
    }

    // 密码规则：长度>8，包含大小写字母、数字、标点符号
    public boolean checkPasswordRule(String password) {
        if (password == null || password.length() <= 8) return false;
        boolean hasUpper = false, hasLower = false, hasNum = false, hasSymbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasNum = true;
            else hasSymbol = true;
        }
        return hasUpper && hasLower && hasNum && hasSymbol;
    }

    // 用户名规则：长度≥5
    public boolean checkUserNameRule(String username) {
        return username != null && username.length() >= 5;
    }

    // 手机号规则：中国大陆手机号，1开头，第二位3-9，共11位数字
    public boolean checkPhoneRule(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return phone.matches("^1[3-9]\\d{9}$");
    }

    // 邮箱规则：标准邮箱格式，用户名@域名.后缀
    public boolean checkEmailRule(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    // 验证用户名+邮箱
    public boolean verifyUserAndEmail(String uname, String mail) {
        return uname.equals(this.name) && mail.equals(this.email);
    }
}
