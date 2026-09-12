import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BinaryPersistence {
    private static final String DATA_FILE = "shop_data.dat";

    public static void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(admins);
            oos.writeObject(customers);
            oos.writeObject(goods);
        } catch (IOException e) {
            System.out.println("保存数据失败：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static DataPack loadAll() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return new DataPack(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return new DataPack(
                    (List<Administrator>) ois.readObject(),
                    (List<Customer>) ois.readObject(),
                    (List<Goods>) ois.readObject()
            );
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("加载数据失败：" + e.getMessage());
            return new DataPack(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    public static class DataPack {
        public List<Administrator> admins;
        public List<Customer> customers;
        public List<Goods> goods;
        public DataPack(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
            this.admins = admins;
            this.customers = customers;
            this.goods = goods;
        }
    }
}

