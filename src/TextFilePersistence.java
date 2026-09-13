import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TextFilePersistence implements Persistence {
    private static final String ADMIN_FILE = "admins.txt";
    private static final String CUSTOMER_FILE = "customers.txt";
    private static final String GOODS_FILE = "goods.txt";
    private static final String ORDERS_FILE = "orders.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        // 保存管理员
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (Administrator admin : admins) {
                bw.write(admin.getId() + "|" + admin.getName() + "|" + admin.getPassword() + "|"
                        + admin.getPhone() + "|" + admin.getEmail() + "|"
                        + admin.getRegisterTime().format(FORMATTER) + "|"
                        + admin.getErrorCount() + "|" + admin.isLock());
                bw.newLine();
            }
        } catch (IOException e) {
            LogUtil.error("保存管理员文本文件失败", e);
        }

        // 保存商品
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(GOODS_FILE))) {
            for (Goods g : goods) {
                bw.write(g.getGoodsId() + "|" + g.getGoodsName() + "|" + g.getManufacturer() + "|"
                        + g.getProduceDate() + "|" + g.getModel() + "|"
                        + g.getPurchasePrice() + "|" + g.getRetailPrice() + "|" + g.getStock());
                bw.newLine();
            }
        } catch (IOException e) {
            LogUtil.error("保存商品文本文件失败", e);
        }

        // 保存顾客和订单
        try (BufferedWriter custBw = new BufferedWriter(new FileWriter(CUSTOMER_FILE));
             BufferedWriter orderBw = new BufferedWriter(new FileWriter(ORDERS_FILE))) {
            for (Customer cust : customers) {
                custBw.write(cust.getId() + "|" + cust.getName() + "|" + cust.getPassword() + "|"
                        + cust.getPhone() + "|" + cust.getEmail() + "|"
                        + cust.getRegisterTime().format(FORMATTER) + "|"
                        + cust.getErrorCount() + "|" + cust.isLock() + "|"
                        + cust.getCustomerRank() + "|" + cust.getTotalConsume() + "|"
                        + (cust.getReceiveAddress() == null ? "" : cust.getReceiveAddress()));
                custBw.newLine();

                for (Order order : cust.getOrderList()) {
                    StringBuilder sb = new StringBuilder();
                    List<CartItem> items = order.getItemList();
                    for (int i = 0; i < items.size(); i++) {
                        CartItem item = items.get(i);
                        sb.append(item.getGoods().getGoodsId()).append(":").append(item.getBuyNumber());
                        if (i < items.size() - 1) sb.append(",");
                    }
                    orderBw.write(order.getOrderNo() + "|" + cust.getId() + "|"
                            + order.getCreateTime().format(FORMATTER) + "|"
                            + order.getTotalAmount() + "|" + order.getPayMode() + "|" + sb);
                    orderBw.newLine();
                }
            }
        } catch (IOException e) {
            LogUtil.error("保存顾客/订单文本文件失败", e);
        }
    }

    @Override
    public DataPack loadAll() {
        List<Administrator> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();

        // 加载管理员
        File adminFile = new File(ADMIN_FILE);
        if (adminFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(adminFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    Administrator admin = new Administrator(
                            Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4],
                            LocalDateTime.parse(parts[5], FORMATTER),
                            Integer.parseInt(parts[6]), Boolean.parseBoolean(parts[7])
                    );
                    admins.add(admin);
                }
            } catch (IOException e) {
                LogUtil.error("读取管理员文本文件失败", e);
            }
        }

        // 加载商品
        File goodsFile = new File(GOODS_FILE);
        if (goodsFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(goodsFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    Goods g = new Goods(
                            Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4],
                            Double.parseDouble(parts[5]), Double.parseDouble(parts[6]), Integer.parseInt(parts[7])
                    );
                    goods.add(g);
                }
            } catch (IOException e) {
                LogUtil.error("读取商品文本文件失败", e);
            }
        }

        // 加载顾客
        File custFile = new File(CUSTOMER_FILE);
        if (custFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(custFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    Customer cust = new Customer(
                            Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4],
                            LocalDateTime.parse(parts[5], FORMATTER),
                            Integer.parseInt(parts[6]), Boolean.parseBoolean(parts[7])
                    );
                    cust.setCustomerRank(Integer.parseInt(parts[8]));
                    cust.setTotalConsume(Double.parseDouble(parts[9]));
                    if (parts.length > 10) cust.setReceiveAddress(parts[10]);
                    customers.add(cust);
                }
            } catch (IOException e) {
                LogUtil.error("读取顾客文本文件失败", e);
            }
        }

        // 加载订单
        File orderFile = new File(ORDERS_FILE);
        if (orderFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(orderFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    int custId = Integer.parseInt(parts[1]);
                    Customer owner = null;
                    for (Customer c : customers) {
                        if (c.getId() == custId) {
                            owner = c;
                            break;
                        }
                    }
                    if (owner == null) continue;

                    String detailStr = parts[5];
                    List<CartItem> itemList = new ArrayList<>();
                    String[] items = detailStr.split(",");
                    for (String item : items) {
                        String[] itemParts = item.split(":");
                        int goodsId = Integer.parseInt(itemParts[0]);
                        int num = Integer.parseInt(itemParts[1]);
                        Goods matchGoods = null;
                        for (Goods g : goods) {
                            if (g.getGoodsId() == goodsId) {
                                matchGoods = g;
                                break;
                            }
                        }
                        if (matchGoods != null) {
                            itemList.add(new CartItem(matchGoods, num));
                        }
                    }

                    Order order = new Order(
                            parts[0], LocalDateTime.parse(parts[2], FORMATTER),
                            itemList, Double.parseDouble(parts[3]), parts[4]
                    );
                    owner.getOrderList().add(order);
                }
            } catch (IOException e) {
                LogUtil.error("读取订单文本文件失败", e);
            }
        }

        return new DataPack(admins, customers, goods);
    }
}
