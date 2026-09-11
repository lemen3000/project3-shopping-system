import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private String orderNo;
    private LocalDateTime createTime;
    private List<CartItem> itemList;
    private double totalAmount;
    private String payMode;

    public Order(String orderNo, LocalDateTime createTime, List<CartItem> itemList, double totalAmount, String payMode) {
        this.orderNo = orderNo;
        this.createTime = createTime;
        this.itemList = itemList;
        this.totalAmount = totalAmount;
        this.payMode = payMode;
    }

    public void printOrderDetail(){
        System.out.println("====订单详情====");
        System.out.println("订单号："+orderNo);
        System.out.println("下单时间："+createTime);
        System.out.println("支付方式："+payMode);
        System.out.println("总金额："+totalAmount);
        for(CartItem ci : itemList){
            System.out.println(ci.getGoods().getGoodsName()+" 数量："+ci.getBuyNumber()+" 小计："+ci.getSubtotal());
        }
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<CartItem> getItemList() { return itemList; }
    public void setItemList(List<CartItem> itemList) { this.itemList = itemList; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getPayMode() { return payMode; }
    public void setPayMode(String payMode) { this.payMode = payMode; }
}