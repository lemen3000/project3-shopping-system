import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.*;

public class LogUtil {
    private static final Logger logger = Logger.getLogger("ShopSystem");

    static {
        try {
            Properties prop = new Properties();
            InputStream is = LogUtil.class.getClassLoader().getResourceAsStream("config.properties");
            if (is != null) {
                prop.load(is);
            }

            boolean logEnable = Boolean.parseBoolean(prop.getProperty("log.enable", "true"));
            String logFile = prop.getProperty("log.file", "shop.log");

            if (logEnable) {
                logger.setUseParentHandlers(false);
                logger.setLevel(Level.ALL);

                // 控制台输出
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.INFO);
                consoleHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(consoleHandler);

                // 文件输出（追加模式）
                FileHandler fileHandler = new FileHandler(logFile, true);
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);
            }
        } catch (IOException e) {
            System.err.println("日志初始化失败：" + e.getMessage());
        }
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void warn(String msg) {
        logger.warning(msg);
    }

    public static void error(String msg, Exception e) {
        logger.log(Level.SEVERE, msg, e);
    }
}

