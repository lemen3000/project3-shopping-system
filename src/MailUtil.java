import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Properties;

public class MailUtil {
    private static String smtpHost;
    private static int smtpPort;
    private static String sender;
    private static String authCode;
    private static boolean enable;

    static {
        try {
            Properties prop = new Properties();
            InputStream is = MailUtil.class.getClassLoader().getResourceAsStream("config.properties");
            if (is != null) {
                prop.load(is);
            }

            enable = Boolean.parseBoolean(prop.getProperty("mail.enable", "false"));
            smtpHost = prop.getProperty("mail.smtp.host", "smtp.qq.com");
            smtpPort = Integer.parseInt(prop.getProperty("mail.smtp.port", "587"));
            sender = prop.getProperty("mail.sender", "");
            authCode = prop.getProperty("mail.auth.code", "");
        } catch (Exception e) {
            LogUtil.error("邮件配置加载失败", e);
            enable = false;
        }
        enable = true;
        smtpHost = "smtp.qq.com";
        smtpPort = 587;
        sender = "qiuchong6@qq.com";
        authCode = "mxhvtjjwqcdkfejj";
    }

    /**
     * 发送普通文本邮件
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文
     */
    public static void sendMail(String toEmail, String subject, String content) {
        if (!enable) {
            LogUtil.info("邮件功能未启用，跳过：" + subject);
            return;
        }

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", smtpHost);
        props.setProperty("mail.smtp.port", String.valueOf(smtpPort));
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, authCode);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setText(content, "UTF-8");
            Transport.send(message);
            LogUtil.info("邮件发送成功，收件人：" + toEmail);
        } catch (MessagingException e) {
            LogUtil.error("邮件发送失败", e);
        }
    }
}

