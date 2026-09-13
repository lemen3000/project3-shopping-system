import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JdbcPersistence {
    // 数据库文件路径，生成在项目根目录
    private static final String DB_URL = "jdbc:sqlite:shop_data.db";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 获取数据库连接
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // 初始化数据库表（不存在则自动创建）
    private static void initTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. 管理员表
            String sqlAdmin = """
                CREATE TABLE IF NOT EXISTS admin (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    password TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    register_time TEXT,
                    error_count INTEGER DEFAULT 0,
                    locked INTEGER DEFAULT 0
                )
                """;
            stmt.execute(sqlAdmin);

            // 2. 顾客表
            String sqlCustomer = """
                CREATE TABLE IF NOT EXISTS customer (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    password TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    register_time TEXT,
                    error_count INTEGER DEFAULT 0,
                    locked INTEGER DEFAULT 0,
                    rank INTEGER DEFAULT 1,
                    total_consume REAL DEFAULT 0,
                    address TEXT
                )
                """;
            stmt.execute(sqlCustomer);

            // 3. 商品表
            String sqlGoods = """
                CREATE TABLE IF NOT EXISTS goods (
                    goods_id INTEGER PRIMARY KEY,
                    goods_name TEXT NOT NULL,
                    manufacturer TEXT,
                    produce_date TEXT,
                    model TEXT,
                    purchase_price REAL,
                    retail_price REAL,
                    stock INTEGER
                )
                """;
            stmt.execute(sqlGoods);

            // 4. 订单表
            String sqlOrder = """
                CREATE TABLE IF NOT EXISTS orders (
                    order_no TEXT PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    create_time TEXT,
                    total_amount REAL,
                    pay_mode TEXT,
                    goods_detail TEXT
                )
                """;
            stmt.execute(sqlOrder);

        } catch (SQLException e) {
            System.out.println("初始化数据库表失败：" + e.getMessage());
        }
    }

    // 全量保存数据到数据库
    public static void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        initTables();
        // 先清空旧数据，保证全量覆盖
        String[] clearTables = {"DELETE FROM admin", "DELETE FROM customer", "DELETE FROM goods", "DELETE FROM orders"};
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : clearTables) {
                stmt.execute(sql);
            }

            // 批量插入管理员
            String insertAdmin = "INSERT INTO admin (id, name, password, phone, email, register_time, error_count, locked) VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertAdmin)) {
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

            // 批量插入顾客
            String insertCustomer = "INSERT INTO customer (id, name, password, phone, email, register_time, error_count, locked, rank, total_consume, address) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertCustomer)) {
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
                    pstmt.setString(11, cust.getReceiveAddress());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 批量插入商品
            String insertGoods = "INSERT INTO goods (goods_id, goods_name, manufacturer, produce_date, model, purchase_price, retail_price, stock) VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertGoods)) {
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

            // 批量插入订单
            String insertOrder = "INSERT INTO orders (order_no, customer_id, create_time, total_amount, pay_mode, goods_detail) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrder)) {
                for (Customer cust : customers) {
                    for (Order order : cust.getOrderList()) {
                        pstmt.setString(1, order.getOrderNo());
                        pstmt.setInt(2, cust.getId());
                        pstmt.setString(3, order.getCreateTime().format(FORMATTER));
                        pstmt.setDouble(4, order.getTotalAmount());
                        pstmt.setString(5, order.getPayMode());
                        // 商品明细序列化为字符串存储
                        StringBuilder sb = new StringBuilder();
                        List<CartItem> items = order.getItemList();
                        for (int i = 0; i < items.size(); i++) {
                            CartItem item = items.get(i);
                            sb.append(item.getGoods().getGoodsId()).append(":").append(item.getBuyNumber());
                            if (i < items.size() - 1) sb.append(",");
                        }
                        pstmt.setString(6, sb.toString());
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
            }

        } catch (SQLException e) {
            System.out.println("保存数据到数据库失败：" + e.getMessage());
        }
    }

    // 从数据库加载所有数据
    public static DataPack loadAll() {
        initTables();
        List<Administrator> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 加载管理员数据
            ResultSet rsAdmin = stmt.executeQuery("SELECT * FROM admin");
            while (rsAdmin.next()) {
                Administrator admin = new Administrator(
                        rsAdmin.getInt("id"),
                        rsAdmin.getString("name"),
                        rsAdmin.getString("password"),
                        rsAdmin.getString("phone"),
                        rsAdmin.getString("email"),
                        LocalDateTime.parse(rsAdmin.getString("register_time"), FORMATTER),
                        rsAdmin.getInt("error_count"),
                        rsAdmin.getInt("locked") == 1
                );
                admins.add(admin);
            }

            // 加载商品数据
            ResultSet rsGoods = stmt.executeQuery("SELECT * FROM goods");
            while (rsGoods.next()) {
                Goods g = new Goods(
                        rsGoods.getInt("goods_id"),
                        rsGoods.getString("goods_name"),
                        rsGoods.getString("manufacturer"),
                        rsGoods.getString("produce_date"),
                        rsGoods.getString("model"),
                        rsGoods.getDouble("purchase_price"),
                        rsGoods.getDouble("retail_price"),
                        rsGoods.getInt("stock")
                );
                goods.add(g);
            }

            // 加载顾客数据
            ResultSet rsCust = stmt.executeQuery("SELECT * FROM customer");
            while (rsCust.next()) {
                Customer cust = new Customer(
                        rsCust.getInt("id"),
                        rsCust.getString("name"),
                        rsCust.getString("password"),
                        rsCust.getString("phone"),
                        rsCust.getString("email"),
                        LocalDateTime.parse(rsCust.getString("register_time"), FORMATTER),
                        rsCust.getInt("error_count"),
                        rsCust.getInt("locked") == 1
                );
                cust.setCustomerRank(rsCust.getInt("rank"));
                cust.setTotalConsume(rsCust.getDouble("total_consume"));
                cust.setReceiveAddress(rsCust.getString("address"));
                customers.add(cust);
            }

            // 加载订单并关联到对应用户
            ResultSet rsOrder = stmt.executeQuery("SELECT * FROM orders");
            while (rsOrder.next()) {
                int custId = rsOrder.getInt("customer_id");
                // 匹配所属顾客
                Customer owner = null;
                for (Customer c : customers) {
                    if (c.getId() == custId) {
                        owner = c;
                        break;
                    }
                }
                if (owner == null) continue;

                // 解析商品明细，匹配商品对象适配CartItem构造
                String detailStr = rsOrder.getString("goods_detail");
                List<CartItem> itemList = new ArrayList<>();
                String[] items = detailStr.split(",");
                for (String item : items) {
                    String[] parts = item.split(":");
                    int goodsId = Integer.parseInt(parts[0]);
                    int num = Integer.parseInt(parts[1]);
                    // 匹配对应商品
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

                // 构造订单对象
                Order order = new Order(
                        rsOrder.getString("order_no"),
                        LocalDateTime.parse(rsOrder.getString("create_time"), FORMATTER),
                        itemList,
                        rsOrder.getDouble("total_amount"),
                        rsOrder.getString("pay_mode")
                );
                owner.getOrderList().add(order);
            }

        } catch (SQLException e) {
            System.out.println("从数据库加载数据失败：" + e.getMessage());
        }

        return new DataPack(admins, customers, goods);
    }

    // 统一数据封装类
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

