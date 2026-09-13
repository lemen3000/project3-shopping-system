import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JdbcPersistence implements Persistence {
    private static final String DB_URL = "jdbc:sqlite:shop_data.db";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initTables(Connection conn) throws SQLException {
        String adminSql = "CREATE TABLE IF NOT EXISTS admin (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "password TEXT NOT NULL," +
                "phone TEXT," +
                "email TEXT," +
                "register_time TEXT," +
                "error_count INTEGER DEFAULT 0," +
                "locked INTEGER DEFAULT 0)";

        String customerSql = "CREATE TABLE IF NOT EXISTS customer (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "password TEXT NOT NULL," +
                "phone TEXT," +
                "email TEXT," +
                "register_time TEXT," +
                "error_count INTEGER DEFAULT 0," +
                "locked INTEGER DEFAULT 0," +
                "rank INTEGER DEFAULT 1," +
                "total_consume REAL DEFAULT 0," +
                "address TEXT)";

        String goodsSql = "CREATE TABLE IF NOT EXISTS goods (" +
                "goods_id INTEGER PRIMARY KEY," +
                "goods_name TEXT NOT NULL," +
                "manufacturer TEXT," +
                "produce_date TEXT," +
                "model TEXT," +
                "purchase_price REAL," +
                "retail_price REAL," +
                "stock INTEGER)";

        String orderSql = "CREATE TABLE IF NOT EXISTS orders (" +
                "order_no TEXT PRIMARY KEY," +
                "customer_id INTEGER NOT NULL," +
                "create_time TEXT," +
                "total_amount REAL," +
                "pay_mode TEXT," +
                "goods_detail TEXT)";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(adminSql);
            stmt.execute(customerSql);
            stmt.execute(goodsSql);
            stmt.execute(orderSql);
        }
    }

    @Override
    public void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        try (Connection conn = getConnection()) {
            initTables(conn);
            // 清空旧数据
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM orders");
                stmt.execute("DELETE FROM customer");
                stmt.execute("DELETE FROM admin");
                stmt.execute("DELETE FROM goods");
            }

            // 插入管理员
            String adminSql = "INSERT INTO admin VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(adminSql)) {
                for (Administrator admin : admins) {
                    pstmt.setInt(1, admin.getId());
                    pstmt.setString(2, admin.getName());
                    pstmt.setString(3, admin.getPassword());
                    pstmt.setString(4, admin.getPhone());
                    pstmt.setString(5, admin.getEmail());
                    pstmt.setString(6, admin.getRegisterTime().format(FORMATTER));
                    pstmt.setInt(7, admin.getErrorCount());
                    pstmt.setInt(8, admin.isLock() ? 1 : 0);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 插入商品
            String goodsSql = "INSERT INTO goods VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(goodsSql)) {
                for (Goods g : goods) {
                    pstmt.setInt(1, g.getGoodsId());
                    pstmt.setString(2, g.getGoodsName());
                    pstmt.setString(3, g.getManufacturer());
                    pstmt.setString(4, g.getProduceDate());
                    pstmt.setString(5, g.getModel());
                    pstmt.setDouble(6, g.getPurchasePrice());
                    pstmt.setDouble(7, g.getRetailPrice());
                    pstmt.setInt(8, g.getStock());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 插入顾客
            String custSql = "INSERT INTO customer VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(custSql)) {
                for (Customer cust : customers) {
                    pstmt.setInt(1, cust.getId());
                    pstmt.setString(2, cust.getName());
                    pstmt.setString(3, cust.getPassword());
                    pstmt.setString(4, cust.getPhone());
                    pstmt.setString(5, cust.getEmail());
                    pstmt.setString(6, cust.getRegisterTime().format(FORMATTER));
                    pstmt.setInt(7, cust.getErrorCount());
                    pstmt.setInt(8, cust.isLock() ? 1 : 0);
                    pstmt.setInt(9, cust.getCustomerRank());
                    pstmt.setDouble(10, cust.getTotalConsume());
                    pstmt.setString(11, cust.getReceiveAddress() == null ? "" : cust.getReceiveAddress());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 插入订单
            String orderSql = "INSERT INTO orders VALUES (?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
                for (Customer cust : customers) {
                    for (Order order : cust.getOrderList()) {
                        StringBuilder sb = new StringBuilder();
                        List<CartItem> items = order.getItemList();
                        for (int i = 0; i < items.size(); i++) {
                            CartItem item = items.get(i);
                            sb.append(item.getGoods().getGoodsId()).append(":").append(item.getBuyNumber());
                            if (i < items.size() - 1) sb.append(",");
                        }
                        pstmt.setString(1, order.getOrderNo());
                        pstmt.setInt(2, cust.getId());
                        pstmt.setString(3, order.getCreateTime().format(FORMATTER));
                        pstmt.setDouble(4, order.getTotalAmount());
                        pstmt.setString(5, order.getPayMode());
                        pstmt.setString(6, sb.toString());
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
            }

        } catch (SQLException e) {
            LogUtil.error("保存SQLite数据库失败", e);
        }
    }

    @Override
    public DataPack loadAll() {
        List<Administrator> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();

        try (Connection conn = getConnection()) {
            initTables(conn);

            // 加载管理员
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM admin")) {
                while (rs.next()) {
                    Administrator admin = new Administrator(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("password"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            LocalDateTime.parse(rs.getString("register_time"), FORMATTER),
                            rs.getInt("error_count"),
                            rs.getInt("locked") == 1
                    );
                    admins.add(admin);
                }
            }

            // 加载商品
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM goods")) {
                while (rs.next()) {
                    Goods g = new Goods(
                            rs.getInt("goods_id"),
                            rs.getString("goods_name"),
                            rs.getString("manufacturer"),
                            rs.getString("produce_date"),
                            rs.getString("model"),
                            rs.getDouble("purchase_price"),
                            rs.getDouble("retail_price"),
                            rs.getInt("stock")
                    );
                    goods.add(g);
                }
            }

            // 加载顾客
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM customer")) {
                while (rs.next()) {
                    Customer cust = new Customer(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("password"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            LocalDateTime.parse(rs.getString("register_time"), FORMATTER),
                            rs.getInt("error_count"),
                            rs.getInt("locked") == 1
                    );
                    cust.setCustomerRank(rs.getInt("rank"));
                    cust.setTotalConsume(rs.getDouble("total_consume"));
                    cust.setReceiveAddress(rs.getString("address"));
                    customers.add(cust);
                }
            }

            // 加载订单
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM orders")) {
                while (rs.next()) {
                    int custId = rs.getInt("customer_id");
                    Customer owner = null;
                    for (Customer c : customers) {
                        if (c.getId() == custId) {
                            owner = c;
                            break;
                        }
                    }
                    if (owner == null) continue;

                    String detailStr = rs.getString("goods_detail");
                    List<CartItem> itemList = new ArrayList<>();
                    String[] items = detailStr.split(",");
                    for (String item : items) {
                        String[] parts = item.split(":");
                        int goodsId = Integer.parseInt(parts[0]);
                        int num = Integer.parseInt(parts[1]);
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
                            rs.getString("order_no"),
                            LocalDateTime.parse(rs.getString("create_time"), FORMATTER),
                            itemList,
                            rs.getDouble("total_amount"),
                            rs.getString("pay_mode")
                    );
                    owner.getOrderList().add(order);
                }
            }

        } catch (SQLException e) {
            LogUtil.error("读取SQLite数据库失败", e);
        }

        return new DataPack(admins, customers, goods);
    }
}
