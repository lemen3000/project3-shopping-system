import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {
    private List<CartItem> itemList = new ArrayList<>();

    public void addItem(Goods goods, int num){
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

    public void updateItemCount(int goodsId, int newNum){
        for(CartItem ci : itemList){
            if(ci.getGoods().getGoodsId() == goodsId){
                ci.setBuyNumber(newNum);
            }
        }
    }

    public double calculateTotal(){
        double sum = 0;
        for(CartItem ci : itemList){
            sum += ci.getSubtotal();
        }
        return sum;
    }

    public void clearCart(){
        itemList.clear();
    }

    public List<CartItem> getItemList() { return itemList; }
}