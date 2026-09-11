public interface UserOperation {
    boolean login(String name, String password);
    void logout();
    boolean modifyPassword(String oldPassword, String newPassword);
}
