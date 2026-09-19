import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class ShoppingSystem {
    private List<Administrator> adminList = new ArrayList<>();
    private List<Customer> customerList = new ArrayList<>();
    private List<Goods> goodsRepository = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);

    private final Persistence persistence;

    public ShoppingSystem() {
        // 通过工厂获取持久化实例
        this.persistence = PersistenceFactory.getPersistence();
        LogUtil.info("系统启动，持久化方式：" + persistence.getClass().getSimpleName());

        // ========== 启动邮件下单服务 ==========
        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                prop.load(is);
            }
        } catch (IOException e) {
            LogUtil.error("读取配置失败", e);
        }
        boolean imapEnable = Boolean.parseBoolean(prop.getProperty("mail.imap.enable", "false"));
        if (imapEnable) {
            new MailOrderService(this).start();
        }
        // ========================================
    }

    public void loadData() {
        DataPack data = persistence.loadAll();
        adminList = data.admins;
        customerList = data.customers;
        goodsRepository = data.goods;

        // 首次运行无数据时初始化默认数据
        if (adminList.isEmpty() && customerList.isEmpty() && goodsRepository.isEmpty()) {
            initDefaultData();
            saveData();
            LogUtil.info("首次运行，初始化默认测试数据");
        }
        LogUtil.info("业务数据加载完成");
    }

    private void initDefaultData() {
        // 默认管理员：admin / ynuinfo#777
        adminList.add(new Administrator(1, "admin", PasswordUtil.encrypt("ynuinfo#777"),
                "13800138000", "admin@qq.com", LocalDateTime.now(), 0, false));
        // 默认顾客：user123 / User@12345
        customerList.add(new Customer(1001, "user123", PasswordUtil.encrypt("User@12345"),
                "13900139000", "user1@qq.com", LocalDateTime.now(), 0, false));
        // 默认商品
        goodsRepository.add(new Goods(101, "笔记本电脑", "联想", "2026-01-01", "model", 5000.0, 6999.0, 10));
        goodsRepository.add(new Goods(102, "手机", "华为", "2026-02-01", "Mate", 3000.0, 4999.0, 20));
    }

    private void saveData() {
        persistence.saveAll(adminList, customerList, goodsRepository);
    }

    // ========== 容错输入辅助方法（修复：输入非数字时提示重试，程序不退出） ==========
    /**
     * 读取一个整数，输入非数字时不会抛出异常导致程序退出。
     *
     * @param tip        提示语
     * @param defaultVal 输入流结束（EOF）时返回的默认值
     * @return 用户输入的整数
     */
    private int readInt(String tip, int defaultVal) {
        while (true) {
            System.out.print(tip);
            try {
                if (!scanner.hasNextLine()) {
                    return defaultVal;
                }
                String text = scanner.nextLine().trim();
                if (text.isEmpty()) {
                    System.out.println("输入不能为空，请输入数字！");
                    continue;
                }
                return Integer.parseInt(text);
            } catch (NumberFormatException | java.util.NoSuchElementException e) {
                System.out.println("输入格式错误，请输入数字！");
            }
        }
    }

    /**
     * 读取一个浮点数，输入非数字时不会抛出异常导致程序退出。
     *
     * @param tip        提示语
     * @param defaultVal 输入流结束（EOF）时返回的默认值
     * @return 用户输入的浮点数
     */
    private double readDouble(String tip, double defaultVal) {
        while (true) {
            System.out.print(tip);
            try {
                if (!scanner.hasNextLine()) {
                    return defaultVal;
                }
                String text = scanner.nextLine().trim();
                if (text.isEmpty()) {
                    System.out.println("输入不能为空，请输入数字！");
                    continue;
                }
                return Double.parseDouble(text);
            } catch (NumberFormatException | java.util.NoSuchElementException e) {
                System.out.println("输入格式错误，请输入正确的数字！");
            }
        }
    }

    /**
     * 读取一个指定范围内的菜单选项，输入非数字或越界时返回 -1 由调用方重试。
     *
     * @param min 最小可选值
     * @param max 最大可选值
     * @return 合法的选项值，非法时返回 -1
     */
    private int readMenuChoice(int min, int max) {
        System.out.print("请输入选择：");
        try {
            if (!scanner.hasNextLine()) {
                return 0;
            }
            String text = scanner.nextLine().trim();
            int value = Integer.parseInt(text);
            if (value < min || value > max) {
                System.out.println("输入无效，请输入 " + min + " 到 " + max + " 之间的数字！");
                return -1;
            }
            return value;
        } catch (NumberFormatException | java.util.NoSuchElementException e) {
            System.out.println("输入格式错误，请输入数字！");
            return -1;
        }
    }
    // ===========================================================================

    // ========== 供邮件下单服务调用的公共方法 ==========
    public List<Customer> getCustomerList() {
        return customerList;
    }

    public List<Goods> getGoodsRepository() {
        return goodsRepository;
    }

    public synchronized void saveDataInternal() {
        persistence.saveAll(adminList, customerList, goodsRepository);
    }

    /**
     * 获取当前持久化方式的名称，用于日志输出
     */
    public String getPersistenceType() {
        return persistence.getClass().getSimpleName();
    }

    // 主菜单
    public void runMainMenu() {
        while (true) {
            System.out.println("\n===== 购物系统主菜单 =====");
            System.out.println("1.注册账号");
            System.out.println("2.登录账号");
            System.out.println("3.忘记密码");
            System.out.println("0.退出系统");
            int op = readMenuChoice(0, 3);
            if (op < 0) {
                continue;
            }
            if (op == 0) {
                saveData();
                LogUtil.info("系统正常退出");
                System.out.println("数据已保存，程序退出！");
                break;
            }
            switch (op) {
                case 1 -> registerMenu();
                case 2 -> loginMenu();
                case 3 -> forgetPasswordMenu();
                default -> System.out.println("输入无效，请重新选择！");
            }
        }
    }

    // 注册（集成日志+邮件）
    private void registerMenu() {
        System.out.println("==== 注册账号 ====");
        System.out.println("1.注册顾客  2.注册管理员");
        int select = readInt("请选择（1.顾客 2.管理员）：", 0);

        if (select != 1 && select != 2) {

            System.out.println("输入无效，请输入 1 或 2！");

            return;

        }

        System.out.print("用户名：");
        String name = scanner.nextLine();
        User tempUser = select == 1 ? new Customer(0, "", "", "", "", null, 0, false) : new Administrator(0, "", "", "", "", null, 0, false);
        if (!tempUser.checkUserNameRule(name)) {
            System.out.println("用户名长度不能少于5个字符！");
            return;
        }

        System.out.print("密码：");
        String pwd = scanner.nextLine();
        if (!tempUser.checkPasswordRule(pwd)) {
            System.out.println("密码长度需大于8位，且包含大小写字母、数字和标点符号！");
            return;
        }

        System.out.print("手机号：");
        String phone = scanner.nextLine();
        if (!tempUser.checkPhoneRule(phone)) {
            System.out.println("手机号格式错误！请输入11位中国大陆手机号（1开头，第二位3-9）");
            return;
        }

        System.out.print("邮箱：");
        String email = scanner.nextLine();
        if (!tempUser.checkEmailRule(email)) {
            System.out.println("邮箱格式错误！请输入正确的邮箱地址（如：example@qq.com）");
            return;
        }

        // 密码加密后存入
        String encryptedPwd = PasswordUtil.encrypt(pwd);

        if (select == 1) {
            int newId = customerList.isEmpty() ? 1001 : customerList.get(customerList.size() - 1).getId() + 1;
            Customer c = new Customer(newId, name, encryptedPwd, phone, email, LocalDateTime.now(), 0, false);
            customerList.add(c);
            saveData();
            LogUtil.info("顾客注册成功，ID：" + newId + "，用户名：" + name);
            MailUtil.sendMail(email, "注册成功通知",
                    "恭喜您注册成功！\n账号ID：" + newId + "\n用户名：" + name);
            System.out.println("顾客注册成功！");
        } else if (select == 2) {
            int newId = adminList.isEmpty() ? 1 : adminList.get(adminList.size() - 1).getId() + 1;
            Administrator a = new Administrator(newId, name, encryptedPwd, phone, email, LocalDateTime.now(), 0, false);
            adminList.add(a);
            saveData();
            LogUtil.info("管理员注册成功，ID：" + newId + "，用户名：" + name);
            MailUtil.sendMail(email, "管理员账号注册成功",
                    "您的管理员账号已创建\n账号：" + name + "\n请妥善保管密码。");
            System.out.println("管理员注册成功！");
        }
    }

    // 忘记密码（集成日志+邮件）
    private void forgetPasswordMenu() {
        System.out.println("==== 忘记密码 ====");
        System.out.print("请输入用户名：");
        String name = scanner.nextLine();
        System.out.print("请输入注册邮箱：");
        String email = scanner.nextLine();

        for (Customer cust : customerList) {
            if (cust.verifyUserAndEmail(name, email)) {
                String newRawPwd = cust.resetRandomPwd();
                cust.setPassword(PasswordUtil.encrypt(newRawPwd));
                cust.setErrorCount(0);
                cust.setLock(false);
                saveData();
                LogUtil.info("顾客密码重置成功，用户名：" + name);
                MailUtil.sendMail(email, "密码重置通知",
                        "您的密码已重置为：" + newRawPwd + "\n请登录后及时修改密码。");
                System.out.println("验证成功！新密码已重置为：" + newRawPwd);
                return;
            }
        }
        for (Administrator admin : adminList) {
            if (admin.verifyUserAndEmail(name, email)) {
                String newRawPwd = admin.resetRandomPwd();
                admin.setPassword(PasswordUtil.encrypt(newRawPwd));
                admin.setErrorCount(0);
                admin.setLock(false);
                saveData();
                LogUtil.info("管理员密码重置成功，用户名：" + name);
                MailUtil.sendMail(email, "管理员密码重置",
                        "您的密码已重置为：" + newRawPwd);
                System.out.println("验证成功！新密码已重置为：" + newRawPwd);
                return;
            }
        }
        LogUtil.warn("密码重置失败，用户名或邮箱不匹配：" + name);
        System.out.println("用户名或邮箱错误，验证失败！");
    }

    // 登录（集成日志+错误计数锁定）
    private void loginMenu() {
        System.out.print("用户名：");
        String name = scanner.nextLine();
        System.out.print("密码：");
        String password = scanner.nextLine();
        boolean loginSuccess = false;

        // 管理员登录校验
        for (Administrator admin : adminList) {
            if (admin.getName().equals(name)) {
                if (admin.isLock()) {
                    LogUtil.warn("锁定账号尝试登录：" + name);
                    System.out.println("该管理员账号已锁定！");
                    return;
                }
                if (PasswordUtil.verify(password, admin.getPassword())) {
                    admin.setErrorCount(0);
                    loginSuccess = true;
                    saveData();
                    LogUtil.info("管理员登录成功：" + name);
                    System.out.println("管理员登录成功！");
                    adminMenu(admin);
                    break;
                } else {
                    admin.setErrorCount(admin.getErrorCount() + 1);
                    if (admin.getErrorCount() >= 3) {
                        admin.setLock(true);
                        saveData();
                        LogUtil.warn("管理员账号锁定：" + name);
                        System.out.println("密码错误3次，账号已被锁定！");
                    } else {
                        saveData();
                        LogUtil.warn("管理员登录失败，剩余次数：" + (3 - admin.getErrorCount()));
                        System.out.println("账号或密码错误！剩余尝试次数：" + (3 - admin.getErrorCount()));
                    }
                    return;
                }
            }
        }

        // 顾客登录校验
        if (!loginSuccess) {
            for (Customer cust : customerList) {
                if (cust.getName().equals(name)) {
                    if (cust.isLock()) {
                        LogUtil.warn("锁定账号尝试登录：" + name);
                        System.out.println("该顾客账号已锁定！");
                        return;
                    }
                    if (PasswordUtil.verify(password, cust.getPassword())) {
                        cust.setErrorCount(0);
                        loginSuccess = true;
                        saveData();
                        LogUtil.info("顾客登录成功：" + name);
                        System.out.println("顾客登录成功！");
                        customerMenu(cust);
                        break;
                    } else {
                        cust.setErrorCount(cust.getErrorCount() + 1);
                        if (cust.getErrorCount() >= 3) {
                            cust.setLock(true);
                            saveData();
                            LogUtil.warn("顾客账号锁定：" + name);
                            System.out.println("密码错误3次，账号已被锁定！");
                        } else {
                            saveData();
                            LogUtil.warn("顾客登录失败，剩余次数：" + (3 - cust.getErrorCount()));
                            System.out.println("账号或密码错误！剩余尝试次数：" + (3 - cust.getErrorCount()));
                        }
                        return;
                    }
                }
            }
        }

        if (!loginSuccess) {
            LogUtil.warn("登录失败，用户名不存在：" + name);
            System.out.println("账号或密码错误！");
        }
    }

    // ========== 管理员菜单 ==========
    private void adminMenu(Administrator admin) {
        while (true) {
            System.out.println("\n==== 管理员菜单 ====");
            System.out.println("1.查看全部商品");
            System.out.println("2.添加商品");
            System.out.println("3.修改商品");
            System.out.println("4.删除商品");
            System.out.println("5.查询商品");
            System.out.println("6.列出所有顾客");
            System.out.println("7.查询顾客");
            System.out.println("8.重置顾客密码");
            System.out.println("9.删除顾客");
            System.out.println("10.修改自身密码");
            System.out.println("0.退出登录");
            int op = readMenuChoice(0, 3);
            if (op < 0) {
                continue;
            }
            if (op == 0) {
                admin.logout();
                break;
            }
            switch (op) {
                case 1 -> admin.listAllGoods(goodsRepository);
                case 2 -> addGoodsMenu(admin);
                case 3 -> updateGoodsMenu(admin);
                case 4 -> deleteGoodsMenu(admin);
                case 5 -> searchGoodsMenu(admin);
                case 6 -> admin.listAllCustomer(customerList);
                case 7 -> searchCustomerMenu(admin);
                case 8 -> resetCustomerPwdMenu(admin);
                case 9 -> deleteCustomerMenu(admin);
                case 10 -> modifyAdminPwdMenu(admin);
            }
        }
    }

    private void addGoodsMenu(Administrator admin) {
        System.out.print("商品ID：");
        int gid = readInt("", -1);
        System.out.print("商品名称：");
        String gname = scanner.nextLine();
        System.out.print("厂商：");
        String factory = scanner.nextLine();
        System.out.print("生产日期：");
        String date = scanner.nextLine();
        System.out.print("型号：");
        String model = scanner.nextLine();
        System.out.print("进货价：");
        double buyP = readDouble("", 0);
        System.out.print("零售价：");
        double sellP = readDouble("", 0);
        System.out.print("库存：");
        int stock = readInt("", 0);
        admin.addGoods(goodsRepository, new Goods(gid, gname, factory, date, model, buyP, sellP, stock));
        saveData();
        LogUtil.info("添加商品成功，ID：" + gid);
    }

    private void updateGoodsMenu(Administrator admin) {
        System.out.print("要修改的商品ID：");
        int gid = readInt("", -1);
        Goods oldGoods = null;
        for (Goods g : goodsRepository) {
            if (g.getGoodsId() == gid) {
                oldGoods = g;
                break;
            }
        }
        if (oldGoods == null) {
            System.out.println("商品不存在！");
            return;
        }
        System.out.println("原商品信息：" + oldGoods.getGoodsInfo());
        System.out.println("请输入新信息（回车不修改）：");

        System.out.print("商品名称：");
        String gname = scanner.nextLine();
        gname = gname.isEmpty() ? oldGoods.getGoodsName() : gname;
        System.out.print("厂商：");
        String factory = scanner.nextLine();
        factory = factory.isEmpty() ? oldGoods.getManufacturer() : factory;
        System.out.print("生产日期：");
        String date = scanner.nextLine();
        date = date.isEmpty() ? oldGoods.getProduceDate() : date;
        System.out.print("型号：");
        String model = scanner.nextLine();
        model = model.isEmpty() ? oldGoods.getModel() : model;
        System.out.print("进货价：");
        String buyPStr = scanner.nextLine();
        double buyP = buyPStr.isEmpty() ? oldGoods.getPurchasePrice() : Double.parseDouble(buyPStr);
        System.out.print("零售价：");
        String sellPStr = scanner.nextLine();
        double sellP = sellPStr.isEmpty() ? oldGoods.getRetailPrice() : Double.parseDouble(sellPStr);
        System.out.print("库存：");
        String stockStr = scanner.nextLine();
        int stock = stockStr.isEmpty() ? oldGoods.getStock() : Integer.parseInt(stockStr);

        if (admin.updateGoods(goodsRepository, new Goods(gid, gname, factory, date, model, buyP, sellP, stock))) {
            System.out.println("修改成功！");
            saveData();
            LogUtil.info("修改商品成功，ID：" + gid);
        }
    }

    private void deleteGoodsMenu(Administrator admin) {
        System.out.print("要删除的商品ID：");
        int gid = readInt("", -1);
        System.out.print("警告：删除后无法恢复，确认删除吗？(y/n)：");
        if (!scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.println("已取消删除");
            return;
        }
        if (admin.deleteGoods(goodsRepository, gid)) {
            System.out.println("删除成功");
            saveData();
            LogUtil.info("删除商品成功，ID：" + gid);
        } else System.out.println("商品不存在");
    }

    private void searchGoodsMenu(Administrator admin) {
        System.out.println("==== 商品查询（回车跳过）====");
        System.out.print("商品名称关键词：");
        String name = scanner.nextLine();
        System.out.print("生产厂家关键词：");
        String manufacturer = scanner.nextLine();
        System.out.print("最低零售价：");
        String minStr = scanner.nextLine();
        Double minPrice = minStr.isEmpty() ? null : Double.parseDouble(minStr);
        System.out.print("最高零售价：");
        String maxStr = scanner.nextLine();
        Double maxPrice = maxStr.isEmpty() ? null : Double.parseDouble(maxStr);

        List<Goods> result = admin.searchGoods(goodsRepository, name, manufacturer, minPrice, maxPrice);
        if (result.isEmpty()) System.out.println("没有找到符合条件的商品");
        else admin.listAllGoods(result);
    }

    private void searchCustomerMenu(Administrator admin) {
        System.out.print("请输入顾客ID或用户名关键词：");
        List<Customer> result = admin.searchCustomer(customerList, scanner.nextLine());
        if (result.isEmpty()) System.out.println("没有找到符合条件的顾客");
        else admin.listAllCustomer(result);
    }

    private void resetCustomerPwdMenu(Administrator admin) {
        System.out.print("请输入顾客ID：");
        int cid = readInt("", -1);
        Customer cust = null;
        for (Customer c : customerList) {
            if (c.getId() == cid) {
                cust = c;
                break;
            }
        }
        if (cust == null) {
            System.out.println("顾客不存在！");
            return;
        }
        String newRawPwd = admin.resetCustomerPassword(cust);
        cust.setPassword(PasswordUtil.encrypt(newRawPwd));
        saveData();
        LogUtil.info("管理员重置顾客密码，ID：" + cid);
        System.out.println("密码重置成功！新密码为：" + newRawPwd);
    }

    private void deleteCustomerMenu(Administrator admin) {
        System.out.print("要删除的顾客ID：");
        int cid = readInt("", -1);
        System.out.print("警告：删除后无法恢复，确认删除吗？(y/n)：");
        if (!scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.println("已取消删除");
            return;
        }
        if (admin.deleteCustomer(customerList, cid)) {
            System.out.println("删除成功");
            saveData();
            LogUtil.info("删除顾客成功，ID：" + cid);
        } else System.out.println("顾客不存在");
    }

    private void modifyAdminPwdMenu(Administrator admin) {
        System.out.print("旧密码：");
        String oldPwd = scanner.nextLine();
        System.out.print("新密码：");
        String newPwd = scanner.nextLine();
        if (PasswordUtil.verify(oldPwd, admin.getPassword()) && admin.checkPasswordRule(newPwd)) {
            admin.setPassword(PasswordUtil.encrypt(newPwd));
            saveData();
            LogUtil.info("管理员修改自身密码成功");
            System.out.println("密码修改成功");
        } else System.out.println("旧密码错误或新密码不符合规则");
    }

    // ========== 顾客菜单 ==========
    private void customerMenu(Customer cust) {
        while (true) {
            System.out.println("\n==== 顾客菜单 ====");
            System.out.println("1.浏览全部商品");
            System.out.println("2.加入购物车");
            System.out.println("3.查看购物车");
            System.out.println("4.修改购物车数量");
            System.out.println("5.移除购物车商品");
            System.out.println("6.结账下单");
            System.out.println("7.查看购物历史");
            System.out.println("8.修改密码");
            System.out.println("0.退出登录");
            int op = readMenuChoice(0, 10);
            if (op < 0) {
                continue;
            }
            if (op == 0) {
                cust.logout();
                break;
            }
            switch (op) {
                case 1 -> {
                    System.out.println("==== 商品列表 ====");
                    for (Goods g : goodsRepository) System.out.println(g.getGoodsInfo());
                }
                case 2 -> addCartMenu(cust);
                case 3 -> showCart(cust);
                case 4 -> updateCartMenu(cust);
                case 5 -> removeCartMenu(cust);
                case 6 -> checkoutMenu(cust);
                case 7 -> cust.showOrderHistory();
                case 8 -> modifyCustPwdMenu(cust);
            }
        }
    }

    private void addCartMenu(Customer cust) {
        System.out.print("商品ID：");
        int gid = readInt("", -1);
        System.out.print("购买数量：");
        int num = readInt("", -1);
        cust.addCart(gid, num, goodsRepository);
    }

    private void showCart(Customer cust) {
        List<CartItem> list = cust.getShoppingCart().getItemList();
        if (list.isEmpty()) System.out.println("购物车为空");
        else {
            System.out.println("==== 购物车 ====");
            for (CartItem ci : list)
                System.out.println(ci.getGoods().getGoodsName() + " 数量：" + ci.getBuyNumber() + " 小计：" + ci.getSubtotal());
            System.out.println("合计：" + cust.getShoppingCart().calculateTotal());
        }
    }

    private void updateCartMenu(Customer cust) {
        System.out.print("商品ID：");
        int gid = readInt("", -1);
        System.out.print("新数量（≤0自动移除）：");
        cust.updateCartItemCount(gid, readInt("", 0));
        System.out.println("修改成功！");
    }

    private void removeCartMenu(Customer cust) {
        System.out.print("要移除的商品ID：");
        int gid = readInt("", -1);
        System.out.print("确认移除吗？(y/n)：");
        if (!scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.println("已取消移除");
            return;
        }
        if (cust.removeCartItem(gid)) System.out.println("移除成功");
        else System.out.println("购物车中没有该商品");
    }

    // 结账下单（含详细订单邮件）
    private void checkoutMenu(Customer cust) {
        if (cust.getShoppingCart().getItemList().isEmpty()) {
            System.out.println("购物车为空！");
            return;
        }
        System.out.println("==== 支付方式 ====");
        System.out.println("1.支付宝  2.微信  3.银行卡");
        System.out.print("请选择：");
        int payOp = readInt("", -1);
        String payMode = switch (payOp) {
            case 1 -> "支付宝";
            case 2 -> "微信";
            case 3 -> "银行卡";
            default -> "";
        };
        if (payOp < 1 || payOp > 3) {
            System.out.println("无效支付方式！");
            return;
        }
        if (cust.checkout(goodsRepository, payMode)) {
            // 获取刚生成的最新订单
            Order latestOrder = cust.getOrderList().get(cust.getOrderList().size() - 1);
            String orderNo = latestOrder.getOrderNo();
            double total = latestOrder.getTotalAmount();

            saveData();
            LogUtil.info("下单成功，订单号：" + orderNo + "，金额：" + total);

            // 拼接详细订单邮件内容
            StringBuilder mailContent = new StringBuilder();
            mailContent.append("您好，您的订单已提交成功！\n\n");
            mailContent.append("订单号：").append(orderNo).append("\n");
            mailContent.append("下单时间：")
                    .append(latestOrder.getCreateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("\n");
            mailContent.append("支付方式：").append(payMode).append("\n");
            mailContent.append("\n-------- 商品明细 --------\n");

            // 遍历订单商品，逐条输出名称、数量、单价、小计
            List<CartItem> items = latestOrder.getItemList();
            for (int i = 0; i < items.size(); i++) {
                CartItem item = items.get(i);
                mailContent.append(i + 1).append(". ")
                        .append(item.getGoods().getGoodsName())
                        .append("  x").append(item.getBuyNumber())
                        .append("  单价：").append(String.format("%.2f", item.getGoods().getRetailPrice())).append("元")
                        .append("  小计：").append(String.format("%.2f", item.getSubtotal())).append("元\n");
            }

            mailContent.append("\n------------------------\n");
            mailContent.append("订单总金额：").append(String.format("%.2f", total)).append(" 元");

            // 发送详细订单邮件
            MailUtil.sendMail(cust.getEmail(), "下单成功通知", mailContent.toString());
            System.out.println("结账成功！订单已生成");
        } else System.out.println("结账失败");
    }

    private void modifyCustPwdMenu(Customer cust) {
        System.out.print("旧密码：");
        String oldPwd = scanner.nextLine();
        System.out.print("新密码：");
        String newPwd = scanner.nextLine();
        if (PasswordUtil.verify(oldPwd, cust.getPassword()) && cust.checkPasswordRule(newPwd)) {
            cust.setPassword(PasswordUtil.encrypt(newPwd));
            saveData();
            LogUtil.info("顾客修改密码成功，用户名：" + cust.getName());
            System.out.println("密码修改成功");
        } else System.out.println("旧密码错误或新密码不符合规则");
    }
}
