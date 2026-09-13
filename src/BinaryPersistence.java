import java.io.*;
import java.util.List;

public class BinaryPersistence implements Persistence {
    private static final String FILE_NAME = "shop_data.dat";

    @Override
    public void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(admins);
            oos.writeObject(customers);
            oos.writeObject(goods);
        } catch (IOException e) {
            LogUtil.error("保存二进制文件失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataPack loadAll() {
        List<Administrator> admins = new java.util.ArrayList<>();
        List<Customer> customers = new java.util.ArrayList<>();
        List<Goods> goods = new java.util.ArrayList<>();

        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                admins = (List<Administrator>) ois.readObject();
                customers = (List<Customer>) ois.readObject();
                goods = (List<Goods>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                LogUtil.error("读取二进制文件失败", e);
            }
        }
        return new DataPack(admins, customers, goods);
    }
}
