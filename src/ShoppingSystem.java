import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.InputMismatchException;

public class ShoppingSystem {
    private List<Administrator> adminList = new ArrayList<>();
    private List<Customer> customerList = new ArrayList<>();
    private List<Goods> goodsRepository = new ArrayList<>();
    private Scanner scanner = new Scanner(System.in);

    public void loadData() {
        BinaryPersistence.DataPack data = BinaryPersistence.loadAll();
        adminList = data.admins;
        customerList = data.customers;
        goodsRepository = data.goods;

        if (adminList.isEmpty() && customerList.isEmpty() && goodsRepository.isEmpty()) {
            initDefaultData();
            saveData();
            System.out.println("首次运行，已初始化默认测试数据");
        }
    }

    private void initDefaultData() {
        adminList.add(new Administrator(1,"admin","ynuinfo#777","13800138000","admin@qq.com",LocalDateTime.now(),0,false));
        customerList.add(new Customer(1001,"user123","User@12345","13900139000","user1@qq.com",LocalDateTime.now(),0,false));
        goodsRepository.add(new Goods(101,"笔记本电脑","联想","2026-01-01","Y7000",4000,5500,20));
        goodsRepository.add(new Goods(102,"手机","华为","2026-02-01","Mate60",3000,4200,30));
    }

    public void saveData() {
        BinaryPersistence.saveAll(adminList, customerList, goodsRepository);
    }

    // 主菜单
    public void runMainMenu(){
        while(true){
            System.out.println("\n===== 购物系统主菜单 =====");
            System.out.println("1.注册账号");
            System.out.println("2.登录账号");
            System.out.println("3.忘记密码");
            System.out.println("0.退出系统");
            System.out.print("请输入选择：");
            int op;
            try{
                op = scanner.nextInt();
                scanner.nextLine();
            }catch(InputMismatchException e){
                System.out.println("输入格式错误，请输入数字！");
                scanner.nextLine();
                continue;
            }
            if(op == 0){
                saveData();
                System.out.println("数据已保存，程序退出！");
                break;
            }
            switch(op){
                case 1 -> registerMenu();
                case 2 -> loginMenu();
                case 3 -> forgetPasswordMenu();
                default -> System.out.println("输入无效，请重新选择！");
            }
        }
    }

    // 注册
    private void registerMenu(){
        System.out.println("====注册====");
        System.out.println("1.注册顾客 2.注册管理员");
        int select;
        try{
            select = scanner.nextInt();
            scanner.nextLine();
        }catch(InputMismatchException e){
            System.out.println("输入错误！");
            scanner.nextLine();
            return;
        }

        System.out.print("用户名：");
        String name = scanner.nextLine();
        User tempUser = select == 1 ? new Customer(0,"","","","",null,0,false) : new Administrator(0,"","","","",null,0,false);
        if(!tempUser.checkUserNameRule(name)){
            System.out.println("用户名长度不能少于5个字符！");
            return;
        }

        System.out.print("密码：");
        String pwd = scanner.nextLine();
        if(!tempUser.checkPasswordRule(pwd)){
            System.out.println("密码长度需大于8位，且包含大小写字母、数字和标点符号！");
            return;
        }

        System.out.print("手机号：");
        String phone = scanner.nextLine();
        System.out.print("邮箱：");
        String email = scanner.nextLine();

        if(select == 1){
            int newId = customerList.isEmpty() ? 1001 : customerList.get(customerList.size()-1).getId() + 1;
            Customer c = new Customer(newId, name, pwd, phone, email, LocalDateTime.now(),0,false);
            customerList.add(c);
            System.out.println("顾客注册成功！");
            saveData();
        }else if(select ==2){
            int newId = adminList.isEmpty() ? 1 : adminList.get(adminList.size()-1).getId() + 1;
            Administrator a = new Administrator(newId, name, pwd, phone, email, LocalDateTime.now(),0,false);
            adminList.add(a);
            System.out.println("管理员注册成功！");
            saveData();
        }
    }

    // 忘记密码
    private void forgetPasswordMenu(){
        System.out.println("====忘记密码====");
        System.out.print("请输入用户名：");
        String name = scanner.nextLine();
        System.out.print("请输入注册邮箱：");
        String email = scanner.nextLine();

        for(Customer cust : customerList){
            if(cust.verifyUserAndEmail(name, email)){
                String newPwd = cust.resetRandomPwd();
                cust.setPassword(newPwd);
                cust.setErrorCount(0);
                cust.setLock(false);
                saveData();
                System.out.println("验证成功！新密码已重置为：" + newPwd);
                return;
            }
        }
        for(Administrator admin : adminList){
            if(admin.verifyUserAndEmail(name, email)){
                String newPwd = admin.resetRandomPwd();
                admin.setPassword(newPwd);
                admin.setErrorCount(0);
                admin.setLock(false);
                saveData();
                System.out.println("验证成功！新密码已重置为：" + newPwd);
                return;
            }
        }
        System.out.println("用户名或邮箱错误，验证失败！");
    }

    // 登录
    private void loginMenu(){
        System.out.print("用户名：");
        String name = scanner.nextLine();
        System.out.print("密码：");
        String password = scanner.nextLine();
        boolean loginSuccess = false;

        for(Administrator admin : adminList){
            if(admin.isLock()){
                System.out.println("该管理员账号已锁定！");
                return;
            }
            if(admin.login(name,password)){
                System.out.println("管理员登录成功！");
                adminMenu(admin);
                loginSuccess = true;
                saveData();
                break;
            }
        }
        if(!loginSuccess){
            for(Customer cust : customerList){
                if(cust.isLock()){
                    System.out.println("该顾客账号已锁定！");
                    return;
                }
                if(cust.login(name,password)){
                    System.out.println("顾客登录成功！");
                    customerMenu(cust);
                    loginSuccess = true;
                    saveData();
                    break;
                }
            }
        }
        if(!loginSuccess){
            System.out.println("账号或密码错误！");
            saveData();
        }
    }

    // 管理员菜单
    private void adminMenu(Administrator admin){
        while(true){
            System.out.println("\n====管理员菜单====");
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
            System.out.print("请输入选择：");
            int op;
            try{
                op = scanner.nextInt();
                scanner.nextLine();
            }catch(InputMismatchException e){
                System.out.println("输入错误！");
                scanner.nextLine();
                continue;
            }
            if(op == 0){
                admin.logout();
                break;
            }
            switch(op){
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

    private void addGoodsMenu(Administrator admin){
        System.out.print("商品ID：");
        int gid = scanner.nextInt();scanner.nextLine();
        System.out.print("商品名称：");
        String gname = scanner.nextLine();
        System.out.print("厂商：");
        String factory = scanner.nextLine();
        System.out.print("生产日期：");
        String date = scanner.nextLine();
        System.out.print("型号：");
        String model = scanner.nextLine();
        System.out.print("进货价：");
        double buyP = scanner.nextDouble();scanner.nextLine();
        System.out.print("零售价：");
        double sellP = scanner.nextDouble();scanner.nextLine();
        System.out.print("库存：");
        int stock = scanner.nextInt();scanner.nextLine();
        admin.addGoods(goodsRepository,new Goods(gid,gname,factory,date,model,buyP,sellP,stock));
        saveData();
    }

    private void updateGoodsMenu(Administrator admin){
        System.out.print("要修改的商品ID：");
        int gid = scanner.nextInt();scanner.nextLine();
        Goods oldGoods = null;
        for(Goods g : goodsRepository){
            if(g.getGoodsId() == gid){ oldGoods = g; break; }
        }
        if(oldGoods == null){ System.out.println("商品不存在！"); return; }
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

        if(admin.updateGoods(goodsRepository,new Goods(gid,gname,factory,date,model,buyP,sellP,stock))){
            System.out.println("修改成功！");
            saveData();
        }
    }

    private void deleteGoodsMenu(Administrator admin){
        System.out.print("要删除的商品ID：");
        int gid = scanner.nextInt();scanner.nextLine();
        System.out.print("警告：删除后无法恢复，确认删除吗？(y/n)：");
        if(!scanner.nextLine().equalsIgnoreCase("y")){
            System.out.println("已取消删除");
            return;
        }
        if(admin.deleteGoods(goodsRepository,gid)){
            System.out.println("删除成功");
            saveData();
        }else System.out.println("商品不存在");
    }

    private void searchGoodsMenu(Administrator admin){
        System.out.println("====商品查询（回车跳过）====");
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
        if(result.isEmpty()) System.out.println("没有找到符合条件的商品");
        else admin.listAllGoods(result);
    }

    private void searchCustomerMenu(Administrator admin){
        System.out.print("请输入顾客ID或用户名关键词：");
        List<Customer> result = admin.searchCustomer(customerList, scanner.nextLine());
        if(result.isEmpty()) System.out.println("没有找到符合条件的顾客");
        else admin.listAllCustomer(result);
    }

    private void resetCustomerPwdMenu(Administrator admin){
        System.out.print("请输入顾客ID：");
        int cid = scanner.nextInt();scanner.nextLine();
        Customer cust = null;
        for(Customer c : customerList){ if(c.getId() == cid){ cust = c; break; }}
        if(cust == null){ System.out.println("顾客不存在！"); return; }
        System.out.println("密码重置成功！新密码为：" + admin.resetCustomerPassword(cust));
        saveData();
    }

    private void deleteCustomerMenu(Administrator admin){
        System.out.print("要删除的顾客ID：");
        int cid = scanner.nextInt();scanner.nextLine();
        System.out.print("警告：删除后无法恢复，确认删除吗？(y/n)：");
        if(!scanner.nextLine().equalsIgnoreCase("y")){
            System.out.println("已取消删除");
            return;
        }
        if(admin.deleteCustomer(customerList,cid)){
            System.out.println("删除成功");
            saveData();
        }else System.out.println("顾客不存在");
    }

    private void modifyAdminPwdMenu(Administrator admin){
        System.out.print("旧密码：");
        String oldPwd = scanner.nextLine();
        System.out.print("新密码：");
        if(admin.modifyPassword(oldPwd,scanner.nextLine())){
            System.out.println("密码修改成功");
            saveData();
        }else System.out.println("旧密码错误或新密码不符合规则");
    }

    // 顾客菜单
    private void customerMenu(Customer cust){
        while(true){
            System.out.println("\n====顾客菜单====");
            System.out.println("1.浏览全部商品");
            System.out.println("2.加入购物车");
            System.out.println("3.查看购物车");
            System.out.println("4.修改购物车数量");
            System.out.println("5.移除购物车商品");
            System.out.println("6.结账下单");
            System.out.println("7.查看购物历史");
            System.out.println("8.修改密码");
            System.out.println("0.退出登录");
            System.out.print("请输入选择：");
            int op;
            try{
                op = scanner.nextInt();
                scanner.nextLine();
            }catch(InputMismatchException e){
                System.out.println("输入错误！");
                scanner.nextLine();
                continue;
            }
            if(op == 0){
                cust.logout();
                break;
            }
            switch(op){
                case 1 -> {
                    System.out.println("====商品列表====");
                    for(Goods g : goodsRepository) System.out.println(g.getGoodsInfo());
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

    private void addCartMenu(Customer cust){
        System.out.print("商品ID：");
        int gid = scanner.nextInt();scanner.nextLine();
        System.out.print("购买数量：");
        cust.addCart(gid,scanner.nextInt(),goodsRepository);
        scanner.nextLine();
    }

    private void showCart(Customer cust){
        List<CartItem> list = cust.getShoppingCart().getItemList();
        if(list.isEmpty()) System.out.println("购物车为空");
        else{
            System.out.println("====购物车====");
            for(CartItem ci : list)
                System.out.println(ci.getGoods().getGoodsName()+" 数量："+ci.getBuyNumber()+" 小计："+ci.getSubtotal());
            System.out.println("合计："+cust.getShoppingCart().calculateTotal());
        }
    }

    private void updateCartMenu(Customer cust){
        System.out.print("商品ID：");
        int gid = scanner.nextInt();scanner.nextLine();
        System.out.print("新数量（≤0自动移除）：");
        cust.updateCartItemCount(gid, scanner.nextInt());
        scanner.nextLine();
        System.out.println("修改成功！");
    }

    private void removeCartMenu(Customer cust){
        System.out.print("要移除的商品ID：");
        int gid = scanner.nextInt();scanner.nextLine();
        System.out.print("确认移除吗？(y/n)：");
        if(!scanner.nextLine().equalsIgnoreCase("y")){
            System.out.println("已取消移除");
            return;
        }
        if(cust.removeCartItem(gid)) System.out.println("移除成功");
        else System.out.println("购物车中没有该商品");
    }

    private void checkoutMenu(Customer cust){
        if(cust.getShoppingCart().getItemList().isEmpty()){
            System.out.println("购物车为空！");
            return;
        }
        System.out.println("====支付方式====");
        System.out.println("1.支付宝 2.微信 3.银行卡");
        System.out.print("请选择：");
        int payOp = scanner.nextInt();scanner.nextLine();
        String payMode = switch(payOp){
            case 1 -> "支付宝"; case 2 -> "微信"; case 3 -> "银行卡"; default -> "";
        };
        if(payOp < 1 || payOp > 3){ System.out.println("无效支付方式！"); return; }
        if(cust.checkout(goodsRepository, payMode)){
            System.out.println("结账成功！订单已生成");
            saveData();
        }else System.out.println("结账失败");
    }

    private void modifyCustPwdMenu(Customer cust){
        System.out.print("旧密码：");
        String oldPwd = scanner.nextLine();
        System.out.print("新密码：");
        if(cust.modifyPassword(oldPwd,scanner.nextLine())){
            System.out.println("密码修改成功");
            saveData();
        }else System.out.println("旧密码错误或新密码不符合规则");
    }
}
