import java.time.LocalDateTime;
import java.util.List;

public class Adiministrator extends User {

    public Adiministrator(int id, String name, String password, String phone, String email, LocalDateTime registerTime, int errorCount, boolean isLock) {
        super(id, name, password, phone, email, registerTime, errorCount, isLock);
    }

    @Override
    public boolean login(String name, String password) {
        if(this.getName().equals(name) && this.getPassword().equals(password)){
            return true;
        }
        return false;
    }

    @Override
    public void logout() {
        System.out.println("管理员退出登录！");
    }

    @Override
    public boolean modifyPassword(String oldPassword, String newPassword) {
        if(oldPassword.equals(this.getPassword())){
            this.setPassword(newPassword);
            return true;
        }
        return false;
    }

    @Override
    public void showInfo() {
        System.out.println("【管理员】账号："+getName()+" 手机号："+getPhone());
    }

    // 商品管理
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
        for(Goods g : goodsList){
            System.out.println(g.getGoodsInfo());
        }
    }

    // 顾客管理
    public void listAllCustomer(List<Customer> custList){
        for(Customer c : custList){
            c.showInfo();
        }
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
