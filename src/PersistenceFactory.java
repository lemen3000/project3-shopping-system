import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PersistenceFactory {
    private static final String CONFIG_FILE = "config.properties";
    private static volatile Persistence instance;

    /**
     * 根据配置文件获取持久化实现实例
     */
    public static Persistence getPersistence() {
        if (instance == null) {
            synchronized (PersistenceFactory.class) {
                if (instance == null) {
                    Properties prop = new Properties();
                    // 从类路径读取配置文件（pom配置后，resources里的文件会在类路径中）
                    try (InputStream is = PersistenceFactory.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                        if (is != null) {
                            prop.load(is);
                        }
                    } catch (IOException e) {
                        LogUtil.error("读取配置文件失败，默认使用文本持久化", e);
                        instance = new TextFilePersistence();
                        return instance;
                    }

                    String type = prop.getProperty("persistence.type", "text");
                    switch (type.toLowerCase()) {
                        case "binary":
                            instance = new BinaryPersistence();
                            break;
                        case "excel":
                            instance = new ExcelPersistence();
                            break;
                        case "sqlite":
                            instance = new JdbcPersistence();
                            break;
                        case "text":
                        default:
                            instance = new TextFilePersistence();
                    }
                }
            }
        }
        return instance;
    }
}
