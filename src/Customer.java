import java.time.LocalDateTime;
import java.util.List;

public class Customer extends User {
    private String receiveAddress;
    private ShoppingCart shoppingCart;
    private List<Order> orderList;
    private int customerRank;
    private double totalConsume;

    public Customer(int id, String name, String password, String phone, String email, LocalDateTime registerTime, int errorCount, boolean isLock) {
        super(id, name, password, phone, email, registerTime, errorCount, isLock);
        this.shoppingCart = new ShoppingCart();
        this.customerRank = 1;
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
        System.out.println("顾客退出登录！");
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
        String rankStr = switch (customerRank) {
            case 1 -> "铜牌顾客";
            case 2 -> "银牌顾客";
            case 3 -> "金牌顾客";
            default -> "未知";
        };
        System.out.println("【顾客】账号："+getName()+" 手机号："+getPhone()+" 等级："+rankStr);
    }

    public void addCart(int goodsId, int num, List<Goods> store){
        for(Goods g : store){
            if(g.getGoodsId() == goodsId){
                shoppingCart.addItem(g, num);
                System.out.println("商品加入购物车成功！");
                return;
            }
        }
        System.out.println("没有找到该商品！");
    }

    public boolean removeCartItem(int goodsId){
        return shoppingCart.removeItem(goodsId);
    }

    public void updateCartItemCount(int goodsId, int newNum){
        shoppingCart.updateItemCount(goodsId, newNum);
    }

    public boolean checkout(List<Goods> store){
        for(CartItem ci : shoppingCart.getItemList()){
            Goods g = ci.getGoods();
            if(g.getStock() < ci.getBuyNumber()){
                System.out.println(g.getGoodsName()+"库存不足！");
                return false;
            }
        }
        for(CartItem ci : shoppingCart.getItemList()){
            Goods g = ci.getGoods();
            g.setStock(g.getStock() - ci.getBuyNumber());
        }
        double total = shoppingCart.calculateTotal();
        this.totalConsume += total;
        if(totalConsume >= 500){
            customerRank = 3;
        }else if(totalConsume >= 200){
            customerRank = 2;
        }
        shoppingCart.clearCart();
        return true;
    }

    public String getReceiveAddress() { return receiveAddress; }
    public void setReceiveAddress(String receiveAddress) { this.receiveAddress = receiveAddress; }
    public ShoppingCart getShoppingCart() { return shoppingCart; }
    public void setShoppingCart(ShoppingCart shoppingCart) { this.shoppingCart = shoppingCart; }
    public List<Order> getOrderList() { return orderList; }
    public void setOrderList(List<Order> orderList) { this.orderList = orderList; }
    public int getCustomerRank() { return customerRank; }
    public void setCustomerRank(int customerRank) { this.customerRank = customerRank; }
    public double getTotalConsume() { return totalConsume; }
    public void setTotalConsume(double totalConsume) { this.totalConsume = totalConsume; }
}
