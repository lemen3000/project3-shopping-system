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

    // 启动加载数据：文件有就读文件，没有就初始化默认数据
    public void loadData() {
        TextFilePersistence.DataPack data = TextFilePersistence.loadAll();
        adminList = data.admins;
        customerList = data.customers;
        goodsRepository = data.goods;

        if (adminList.isEmpty() && customerList.isEmpty() && goodsRepository.isEmpty()) {
            initDefaultData();
            saveData();
            System.out.println("首次运行，已初始化默认测试数据");
        }
    }

    // 默认测试数据
    private void initDefaultData() {
        adminList.add(new Administrator(1,"admin","ynuinfo#777","13800138000","admin@qq.com",LocalDateTime.now(),0,false));
        customerList.add(new Customer(1001,"user1","123456","13900139000","user1@qq.com",LocalDateTime.now(),0,false));
        goodsRepository.add(new Goods(101,"笔记本电脑","联想","2026-01-01","Y7000",4000,5500,20));
        goodsRepository.add(new Goods(102,"手机","华为","2026-02-01","Mate60",3000,4200,30));
    }

    // 退出保存数据到文本文件
    public void saveData() {
        TextFilePersistence.saveAll(adminList, customerList, goodsRepository);
    }

    public void runMainMenu(){
        while(true){
            System.out.println("\n===== 购物系统主菜单 =====");
            System.out.println("1.注册账号");
            System.out.println("2.登录账号");
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
            if(op == 1){
                registerMenu();
            }else if(op == 2){
                loginMenu();
            }else{
                System.out.println("输入无效，请重新选择！");
            }
        }
    }

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
        System.out.print("密码：");
        String pwd = scanner.nextLine();
        System.out.print("手机号：");
        String phone = scanner.nextLine();
        System.out.print("邮箱：");
        String email = scanner.nextLine();

        if(select == 1){
            int newId = customerList.size()+1001;
            Customer c = new Customer(newId, name, pwd, phone, email, LocalDateTime.now(),0,false);
            customerList.add(c);
            System.out.println("顾客注册成功！");
        }else if(select ==2){
            int newId = adminList.size()+1;
            Administrator a = new Administrator(newId, name, pwd, phone, email, LocalDateTime.now(),0,false);
            adminList.add(a);
            System.out.println("管理员注册成功！");
        }
    }

    private void loginMenu(){
        System.out.print("用户名：");
        String name = scanner.nextLine();
        System.out.print("密码：");
        String password = scanner.nextLine();
        boolean loginSuccess = false;

        for(Administrator admin : adminList){
            if(admin.isLock()){
                System.out.println("该管理员账号已锁定！");
                continue;
            }
            if(admin.login(name,password)){
                adminMenu(admin);
                loginSuccess = true;
                break;
            }
        }
        if(!loginSuccess){
            for(Customer cust : customerList){
                if(cust.isLock()){
                    System.out.println("该顾客账号已锁定！");
                    continue;
                }
                if(cust.login(name,password)){
                    customerMenu(cust);
                    loginSuccess = true;
                    break;
                }
            }
        }
        if(!loginSuccess){
            System.out.println("账号或密码错误！");
        }
    }

    private void adminMenu(Administrator admin){
        while(true){
            System.out.println("\n====管理员菜单====");
            System.out.println("1.查看全部商品");
            System.out.println("2.添加商品");
            System.out.println("3.删除商品");
            System.out.println("4.修改密码");
            System.out.println("0.退出登录");
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
            if(op ==1){
                admin.listAllGoods(goodsRepository);
            }else if(op ==2){
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
                Goods g = new Goods(gid,gname,factory,date,model,buyP,sellP,stock);
                admin.addGoods(goodsRepository,g);
            }else if(op ==3){
                System.out.print("要删除的商品ID：");
                int gid = scanner.nextInt();scanner.nextLine();
                boolean res = admin.deleteGoods(goodsRepository,gid);
                if(res) System.out.println("删除成功");
                else System.out.println("商品不存在");
            }else if(op ==4){
                System.out.print("旧密码：");
                String oldPwd = scanner.nextLine();
                System.out.print("新密码：");
                String newPwd = scanner.nextLine();
                boolean res = admin.modifyPassword(oldPwd,newPwd);
                if(res) System.out.println("密码修改成功");
                else System.out.println("旧密码错误");
            }
        }
    }

    private void customerMenu(Customer cust){
        while(true){
            System.out.println("\n====顾客菜单====");
            System.out.println("1.浏览全部商品");
            System.out.println("2.加入购物车");
            System.out.println("3.查看购物车");
            System.out.println("4.结账下单");
            System.out.println("5.修改密码");
            System.out.println("0.退出登录");
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
            if(op ==1){
                for(Goods g : goodsRepository){
                    System.out.println(g.getGoodsInfo());
                }
            }else if(op ==2){
                System.out.print("商品ID：");
                int gid = scanner.nextInt();scanner.nextLine();
                System.out.print("购买数量：");
                int num = scanner.nextInt();scanner.nextLine();
                cust.addCart(gid,num,goodsRepository);
            }else if(op ==3){
                List<CartItem> list = cust.getShoppingCart().getItemList();
                if(list.isEmpty()){
                    System.out.println("购物车为空");
                }else{
                    for(CartItem ci : list){
                        System.out.println(ci.getGoods().getGoodsName()+" 数量："+ci.getBuyNumber()+"小计："+ci.getSubtotal());
                    }
                    System.out.println("购物车合计："+cust.getShoppingCart().calculateTotal());
                }
            }else if(op ==4){
                boolean ok = cust.checkout(goodsRepository);
                if(ok){
                    System.out.println("结账成功！订单生成");
                }else{
                    System.out.println("结账失败");
                }
            }else if(op ==5){
                System.out.print("旧密码：");
                String oldPwd = scanner.nextLine();
                System.out.print("新密码：");
                String newPwd = scanner.nextLine();
                boolean res = cust.modifyPassword(oldPwd,newPwd);
                if(res) System.out.println("密码修改成功");
                else System.out.println("旧密码错误");
            }
        }
    }
}
