import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ShoppingCart implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<CartItem> itemList = new ArrayList<>();

    // 添加商品，重复则累加数量
    public void addItem(Goods goods, int num){
        for(CartItem ci : itemList){
            if(ci.getGoods().getGoodsId() == goods.getGoodsId()){
                ci.setBuyNumber(ci.getBuyNumber() + num);
                return;
            }
        }
        itemList.add(new CartItem(goods, num));
    }

    public boolean removeItem(int goodsId){
        for(CartItem ci : itemList){
            if(ci.getGoods().getGoodsId() == goodsId){
                itemList.remove(ci);
                return true;
            }
        }
        return false;
    }

    // 修改数量，≤0则自动移除
    public void updateItemCount(int goodsId, int newNum){
        for(int i = 0; i < itemList.size(); i++){
            CartItem ci = itemList.get(i);
            if(ci.getGoods().getGoodsId() == goodsId){
                if(newNum <= 0) itemList.remove(i);
                else ci.setBuyNumber(newNum);
                return;
            }
        }
    }

    public double calculateTotal(){
        double sum = 0;
        for(CartItem ci : itemList) sum += ci.getSubtotal();
        return sum;
    }

    public void clearCart(){ itemList.clear(); }
    public List<CartItem> getItemList() { return itemList; }
}
