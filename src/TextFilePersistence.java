import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TextFilePersistence {
    private static final String ADMIN_FILE = "admins.txt";
    private static final String CUSTOMER_FILE = "customers.txt";
    private static final String GOODS_FILE = "goods.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 保存全部数据
    public static void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        saveAdmins(admins);
        saveCustomers(customers);
        saveGoods(goods);
    }

    // 加载全部数据
    public static DataPack loadAll() {
        return new DataPack(loadAdmins(), loadCustomers(), loadGoods());
    }

    private static void saveAdmins(List<Administrator> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (Administrator admin : list) {
                String line = String.format("%d|%s|%s|%s|%s|%s|%d|%b",
                        admin.getId(), admin.getName(), admin.getPassword(),
                        admin.getPhone(), admin.getEmail(),
                        admin.getRegisterTime().format(FORMATTER),
                        admin.getErrorCount(), admin.isLock());
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("保存管理员失败：" + e.getMessage());
        }
    }

    private static List<Administrator> loadAdmins() {
        List<Administrator> list = new ArrayList<>();
        File file = new File(ADMIN_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split("\\|");
                list.add(new Administrator(
                        Integer.parseInt(arr[0]), arr[1], arr[2], arr[3], arr[4],
                        LocalDateTime.parse(arr[5], FORMATTER),
                        Integer.parseInt(arr[6]), Boolean.parseBoolean(arr[7])
                ));
            }
        } catch (IOException e) {
            System.out.println("加载管理员失败：" + e.getMessage());
        }
        return list;
    }

    private static void saveCustomers(List<Customer> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CUSTOMER_FILE))) {
            for (Customer cust : list) {
                String line = String.format("%d|%s|%s|%s|%s|%s|%d|%b|%d|%.2f",
                        cust.getId(), cust.getName(), cust.getPassword(),
                        cust.getPhone(), cust.getEmail(),
                        cust.getRegisterTime().format(FORMATTER),
                        cust.getErrorCount(), cust.isLock(),
                        cust.getCustomerRank(), cust.getTotalConsume());
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("保存顾客失败：" + e.getMessage());
        }
    }

    private static List<Customer> loadCustomers() {
        List<Customer> list = new ArrayList<>();
        File file = new File(CUSTOMER_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split("\\|");
                Customer cust = new Customer(
                        Integer.parseInt(arr[0]), arr[1], arr[2], arr[3], arr[4],
                        LocalDateTime.parse(arr[5], FORMATTER),
                        Integer.parseInt(arr[6]), Boolean.parseBoolean(arr[7])
                );
                cust.setCustomerRank(Integer.parseInt(arr[8]));
                cust.setTotalConsume(Double.parseDouble(arr[9]));
                list.add(cust);
            }
        } catch (IOException e) {
            System.out.println("加载顾客失败：" + e.getMessage());
        }
        return list;
    }

    private static void saveGoods(List<Goods> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(GOODS_FILE))) {
            for (Goods g : list) {
                String line = String.format("%d|%s|%s|%s|%s|%.2f|%.2f|%d",
                        g.getGoodsId(), g.getGoodsName(), g.getManufacturer(),
                        g.getProduceDate(), g.getModel(),
                        g.getPurchasePrice(), g.getRetailPrice(), g.getStock());
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("保存商品失败：" + e.getMessage());
        }
    }

    private static List<Goods> loadGoods() {
        List<Goods> list = new ArrayList<>();
        File file = new File(GOODS_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split("\\|");
                list.add(new Goods(
                        Integer.parseInt(arr[0]), arr[1], arr[2], arr[3], arr[4],
                        Double.parseDouble(arr[5]), Double.parseDouble(arr[6]),
                        Integer.parseInt(arr[7])
                ));
            }
        } catch (IOException e) {
            System.out.println("加载商品失败：" + e.getMessage());
        }
        return list;
    }

    // 数据封装类
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
