import java.util.List;

public class DataPack {
    public List<Administrator> admins;
    public List<Customer> customers;
    public List<Goods> goods;

    public DataPack(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        this.admins = admins;
        this.customers = customers;
        this.goods = goods;
    }
}
