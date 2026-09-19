import javax.mail.*;
import java.io.InputStream;
import java.util.Properties;

/**
 * IMAP连接测试工具：从 config.properties 读取真实配置，测试系统邮箱能否正常登录。
 * 注意：这只是连接测试，不会处理下单邮件。要使用邮件下单功能，请运行 Main 主程序。
 */
public class ImapTest {
    public static void main(String[] args) {
        // 从配置文件读取真实参数，不再硬编码占位符
        Properties config = new Properties();
        try (InputStream is = ImapTest.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (Exception e) {
            System.out.println("读取配置文件失败：" + e.getMessage());
            return;
        }

        String email = config.getProperty("mail.sender", "");
        String authCode = config.getProperty("mail.auth.code", "");
        String imapHost = config.getProperty("mail.imap.host", "imap.qq.com");
        String imapPort = config.getProperty("mail.imap.port", "993");

        if (email.isEmpty() || authCode.isEmpty()) {
            System.out.println("配置文件中 mail.sender 或 mail.auth.code 为空，请检查 config.properties");
            return;
        }

        System.out.println("正在使用配置中的账号测试连接：" + email);
        System.out.println("正在连接 " + imapHost + ":" + imapPort + " ...");

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", imapHost);
        props.setProperty("mail.imaps.port", imapPort);
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.ssl.trust", imapHost);
        props.setProperty("mail.imaps.connectiontimeout", "10000");
        props.setProperty("mail.imaps.timeout", "10000");

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(email, authCode);
            System.out.println("✅ IMAP连接成功！账号授权码正常");

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            System.out.println("收件箱邮件总数：" + inbox.getMessageCount());
            inbox.close(false);
            store.close();
            System.out.println("\n提示：连接成功后，运行 Main 主程序即可启动邮件下单功能。");
        } catch (AuthenticationFailedException e) {
            System.out.println("❌ 认证失败：账号或授权码错误");
            System.out.println("请检查 config.properties 中的 mail.sender 和 mail.auth.code 是否正确");
            System.out.println("授权码需要在QQ邮箱设置中开启IMAP/SMTP服务后生成，不是QQ登录密码");
        } catch (MessagingException e) {
            System.out.println("❌ 连接异常：" + e.getMessage());
        }
    }
}
