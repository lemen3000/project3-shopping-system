public class Goods {
    private int goodsId;
    private String goodsName;
    private String manufacturer;
    private String produceDate;
    private String model;
    private double purchasePrice;
    private double retailPrice;
    private int stock;

    public Goods(int goodsId, String goodsName, String manufacturer, String produceDate, String model, double purchasePrice, double retailPrice, int stock) {
        this.goodsId = goodsId;
        this.goodsName = goodsName;
        this.manufacturer = manufacturer;
        this.produceDate = produceDate;
        this.model = model;
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.stock = stock;
    }

    public String getGoodsInfo(){
        return "商品ID:"+goodsId+" 名称:"+goodsName+" 零售价:"+retailPrice+" 库存:"+stock;
    }

    public int getGoodsId() { return goodsId; }
    public void setGoodsId(int goodsId) { this.goodsId = goodsId; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getProduceDate() { return produceDate; }
    public void setProduceDate(String produceDate) { this.produceDate = produceDate; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    public double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
