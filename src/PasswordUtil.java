import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    private static final String SEPARATOR = "$";

    // 生成16位随机盐值
    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // 带盐的SHA-256哈希计算
    private static String hash(String input, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.reset();
            digest.update(salt.getBytes());
            byte[] hashBytes = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("密码哈希计算失败", e);
        }
    }

    /**
     * 加密明文密码，返回 盐$哈希 格式的加密字符串
     * @param rawPassword 明文密码
     * @return 加密后的存储串
     */
    public static String encrypt(String rawPassword) {
        String salt = generateSalt();
        String hashResult = hash(rawPassword, salt);
        return salt + SEPARATOR + hashResult;
    }

    /**
     * 验证密码是否匹配
     * @param rawPassword 用户输入的明文密码
     * @param storedPassword 存储的加密串
     * @return 密码是否正确
     */
    public static boolean verify(String rawPassword, String storedPassword) {
        if (storedPassword == null || !storedPassword.contains(SEPARATOR)) {
            return false;
        }
        String[] parts = storedPassword.split("\\" + SEPARATOR);
        String salt = parts[0];
        String storedHash = parts[1];
        String inputHash = hash(rawPassword, salt);
        return inputHash.equals(storedHash);
    }
}
