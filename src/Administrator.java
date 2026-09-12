import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Administrator extends User {

    public Administrator(int id, String name, String password, String phone, String email, LocalDateTime registerTime, int errorCount, boolean isLock) {
        super(id, name, password, phone, email, registerTime, errorCount, isLock);
    }

    @Override
    public boolean login(String name, String password) {
        if(this.getName().equals(name) && this.getPassword().equals(password)){
            this.setErrorCount(0);
            return true;
        }
        this.setErrorCount(this.getErrorCount() + 1);
        if(this.getErrorCount() >= 3) this.setLock(true);
        return false;
    }

    @Override
    public void logout() { System.out.println("管理员退出登录！"); }

    @Override
    public boolean modifyPassword(String oldPassword, String newPassword) {
        if(oldPassword.equals(this.getPassword())){
            if(!checkPasswordRule(newPassword)){
                System.out.println("新密码不符合规则！");
                return false;
            }
            this.setPassword(newPassword);
            return true;
        }
        return false;
    }

    @Override
    public void showInfo() {
        System.out.println("【管理员】账号："+getName()+" 手机号："+getPhone()+" 邮箱："+getEmail());
    }

    // ===== 商品管理 =====
    public void addGoods(List<Goods> goodsList, Goods g){
        goodsList.add(g);
        System.out.println("商品添加成功！");
    }

    public boolean deleteGoods(List<Goods> goodsList, int goodsId){
        for(Goods g : goodsList){
            if(g.getGoodsId() == goodsId){
                goodsList.remove(g);
                return true;
            }
        }
        return false;
    }

    public boolean updateGoods(List<Goods> goodsList, Goods g){
        for(int i = 0; i < goodsList.size(); i++){
            if(goodsList.get(i).getGoodsId() == g.getGoodsId()){
                goodsList.set(i,g);
                return true;
            }
        }
        return false;
    }

    public void listAllGoods(List<Goods> goodsList){
        if(goodsList.isEmpty()){
            System.out.println("暂无商品！");
            return;
        }
        System.out.printf("%-8s%-12s%-10s%-12s%-10s%-10s%-10s%-6s%n",
                "商品ID","商品名称","生产厂家","生产日期","型号","进货价","零售价","库存");
        System.out.println("----------------------------------------------------------------");
        for(Goods g : goodsList){
            System.out.printf("%-8d%-12s%-10s%-12s%-10s%-10.2f%-10.2f%-6d%n",
                    g.getGoodsId(), g.getGoodsName(), g.getManufacturer(),
                    g.getProduceDate(), g.getModel(), g.getPurchasePrice(),
                    g.getRetailPrice(), g.getStock());
        }
    }

    // 组合查询商品：名称、厂家、价格范围
    public List<Goods> searchGoods(List<Goods> goodsList, String nameKeyword, String manufacturerKeyword, Double minPrice, Double maxPrice){
        List<Goods> result = new ArrayList<>();
        for(Goods g : goodsList){
            boolean match = true;
            if(nameKeyword != null && !nameKeyword.isEmpty())
                if(!g.getGoodsName().contains(nameKeyword)) match = false;
            if(manufacturerKeyword != null && !manufacturerKeyword.isEmpty())
                if(!g.getManufacturer().contains(manufacturerKeyword)) match = false;
            if(minPrice != null && g.getRetailPrice() < minPrice) match = false;
            if(maxPrice != null && g.getRetailPrice() > maxPrice) match = false;
            if(match) result.add(g);
        }
        return result;
    }

    // ===== 顾客管理 =====
    public void listAllCustomer(List<Customer> custList){
        if(custList.isEmpty()){
            System.out.println("暂无顾客！");
            return;
        }
        System.out.printf("%-6s%-12s%-8s%-20s%-12s%-12s%-18s%n",
                "ID","用户名","等级","注册时间","累计消费","手机号","邮箱");
        System.out.println("-----------------------------------------------------------------------");
        for(Customer c : custList){
            String rankStr = switch (c.getCustomerRank()) {
                case 1 -> "铜牌"; case 2 -> "银牌"; case 3 -> "金牌"; default -> "未知";
            };
            System.out.printf("%-6d%-12s%-8s%-20s%-12.2f%-12s%-18s%n",
                    c.getId(), c.getName(), rankStr, c.getRegisterTime().toString(),
                    c.getTotalConsume(), c.getPhone(), c.getEmail());
        }
    }

    // 查询顾客：按ID或用户名模糊查询
    public List<Customer> searchCustomer(List<Customer> custList, String keyword){
        List<Customer> result = new ArrayList<>();
        for(Customer c : custList){
            try{
                if(c.getId() == Integer.parseInt(keyword)){
                    result.add(c);
                    continue;
                }
            }catch(NumberFormatException e){}
            if(c.getName().contains(keyword)) result.add(c);
        }
        return result;
    }

    // 重置顾客密码
    public String resetCustomerPassword(Customer cust){
        String newPwd = cust.resetRandomPwd();
        cust.setPassword(newPwd);
        cust.setErrorCount(0);
        cust.setLock(false);
        return newPwd;
    }

    public boolean deleteCustomer(List<Customer> custList, int cid){
        for(Customer c : custList){
            if(c.getId() == cid){
                custList.remove(c);
                return true;
            }
        }
        return false;
    }
}

