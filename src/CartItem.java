public class CartItem {
    private Goods goods;
    private int buyNumber;

    public CartItem(Goods goods, int buyNumber) {
        this.goods = goods;
        this.buyNumber = buyNumber;
    }

    public double getSubtotal(){
        return goods.getRetailPrice() * buyNumber;
    }

    public Goods getGoods() { return goods; }
    public void setGoods(Goods goods) { this.goods = goods; }
    public int getBuyNumber() { return buyNumber; }
    public void setBuyNumber(int buyNumber) { this.buyNumber = buyNumber; }
}