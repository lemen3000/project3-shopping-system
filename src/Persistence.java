import java.util.List;

public interface Persistence {
    void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods);
    DataPack loadAll();
}

