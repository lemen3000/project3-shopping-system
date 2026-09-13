import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExcelPersistence {
    private static final String FILE_NAME = "shop_data.xlsx";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== 保存：内存数据写入Excel文件 ==========
    public static void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 一个Excel文件创建4个工作表
            createAdminSheet(workbook, admins);
            createCustomerSheet(workbook, customers);
            createGoodsSheet(workbook, goods);
            createOrderSheet(workbook, customers);

            // 写入到本地文件
            try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            System.out.println("保存Excel失败：" + e.getMessage());
        }
    }

    // 管理员工作表
    private static void createAdminSheet(Workbook workbook, List<Administrator> list) {
        Sheet sheet = workbook.createSheet("管理员");
        String[] headers = {"ID", "用户名", "密码", "手机号", "邮箱", "注册时间", "错误次数", "是否锁定"};
        createHeader(sheet, headers);

        int rowNum = 1;
        for (Administrator admin : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(admin.getId());
            row.createCell(1).setCellValue(admin.getName());
            row.createCell(2).setCellValue(admin.getPassword());
            row.createCell(3).setCellValue(admin.getPhone());
            row.createCell(4).setCellValue(admin.getEmail());
            row.createCell(5).setCellValue(admin.getRegisterTime().format(FORMATTER));
            row.createCell(6).setCellValue(admin.getErrorCount());
            row.createCell(7).setCellValue(admin.isLock());
        }
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    // 顾客工作表
    private static void createCustomerSheet(Workbook workbook, List<Customer> list) {
        Sheet sheet = workbook.createSheet("顾客");
        String[] headers = {"ID", "用户名", "密码", "手机号", "邮箱", "注册时间", "错误次数", "是否锁定", "等级", "累计消费", "收货地址"};
        createHeader(sheet, headers);

        int rowNum = 1;
        for (Customer cust : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(cust.getId());
            row.createCell(1).setCellValue(cust.getName());
            row.createCell(2).setCellValue(cust.getPassword());
            row.createCell(3).setCellValue(cust.getPhone());
            row.createCell(4).setCellValue(cust.getEmail());
            row.createCell(5).setCellValue(cust.getRegisterTime().format(FORMATTER));
            row.createCell(6).setCellValue(cust.getErrorCount());
            row.createCell(7).setCellValue(cust.isLock());
            row.createCell(8).setCellValue(cust.getCustomerRank());
            row.createCell(9).setCellValue(cust.getTotalConsume());
            row.createCell(10).setCellValue(cust.getReceiveAddress() == null ? "" : cust.getReceiveAddress());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    // 商品工作表
    private static void createGoodsSheet(Workbook workbook, List<Goods> list) {
        Sheet sheet = workbook.createSheet("商品");
        String[] headers = {"商品ID", "商品名称", "生产厂家", "生产日期", "型号", "进货价", "零售价", "库存"};
        createHeader(sheet, headers);

        int rowNum = 1;
        for (Goods g : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(g.getGoodsId());
            row.createCell(1).setCellValue(g.getGoodsName());
            row.createCell(2).setCellValue(g.getManufacturer());
            row.createCell(3).setCellValue(g.getProduceDate());
            row.createCell(4).setCellValue(g.getModel());
            row.createCell(5).setCellValue(g.getPurchasePrice());
            row.createCell(6).setCellValue(g.getRetailPrice());
            row.createCell(7).setCellValue(g.getStock());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    // 订单工作表
    private static void createOrderSheet(Workbook workbook, List<Customer> customers) {
        Sheet sheet = workbook.createSheet("订单");
        String[] headers = {"订单号", "顾客ID", "下单时间", "总金额", "支付方式", "商品明细"};
        createHeader(sheet, headers);

        int rowNum = 1;
        for (Customer cust : customers) {
            for (Order order : cust.getOrderList()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getOrderNo());
                row.createCell(1).setCellValue(cust.getId());
                row.createCell(2).setCellValue(order.getCreateTime().format(FORMATTER));
                row.createCell(3).setCellValue(order.getTotalAmount());
                row.createCell(4).setCellValue(order.getPayMode());

                // 商品明细格式：商品ID:数量,商品ID:数量
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < order.getItemList().size(); i++) {
                    CartItem ci = order.getItemList().get(i);
                    sb.append(ci.getGoods().getGoodsId()).append(":").append(ci.getBuyNumber());
                    if (i < order.getItemList().size() - 1) sb.append(",");
                }
                row.createCell(5).setCellValue(sb.toString());
            }
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    // 通用表头：加粗样式
    private static void createHeader(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    // ========== 加载：Excel文件读取到内存 ==========
    public static DataPack loadAll() {
        File file = new File(FILE_NAME);
        // 文件不存在则返回空集合
        if (!file.exists()) {
            return new DataPack(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        try (Workbook workbook = WorkbookFactory.create(file)) {
            // 按依赖顺序加载：商品 → 管理员 → 顾客 → 订单
            List<Goods> goods = loadGoods(workbook);
            List<Administrator> admins = loadAdmins(workbook);
            List<Customer> customers = loadCustomers(workbook);
            loadOrders(workbook, customers, goods);
            return new DataPack(admins, customers, goods);
        } catch (Exception e) {
            System.out.println("加载Excel失败：" + e.getMessage());
            return new DataPack(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    private static List<Administrator> loadAdmins(Workbook workbook) {
        List<Administrator> list = new ArrayList<>();
        Sheet sheet = workbook.getSheet("管理员");
        if (sheet == null) return list;
        // 第0行是表头，从第1行开始读取数据
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            list.add(new Administrator(
                    (int) row.getCell(0).getNumericCellValue(),
                    row.getCell(1).getStringCellValue(),
                    row.getCell(2).getStringCellValue(),
                    row.getCell(3).getStringCellValue(),
                    row.getCell(4).getStringCellValue(),
                    LocalDateTime.parse(row.getCell(5).getStringCellValue(), FORMATTER),
                    (int) row.getCell(6).getNumericCellValue(),
                    row.getCell(7).getBooleanCellValue()
            ));
        }
        return list;
    }

    private static List<Customer> loadCustomers(Workbook workbook) {
        List<Customer> list = new ArrayList<>();
        Sheet sheet = workbook.getSheet("顾客");
        if (sheet == null) return list;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Customer cust = new Customer(
                    (int) row.getCell(0).getNumericCellValue(),
                    row.getCell(1).getStringCellValue(),
                    row.getCell(2).getStringCellValue(),
                    row.getCell(3).getStringCellValue(),
                    row.getCell(4).getStringCellValue(),
                    LocalDateTime.parse(row.getCell(5).getStringCellValue(), FORMATTER),
                    (int) row.getCell(6).getNumericCellValue(),
                    row.getCell(7).getBooleanCellValue()
            );
            cust.setCustomerRank((int) row.getCell(8).getNumericCellValue());
            cust.setTotalConsume(row.getCell(9).getNumericCellValue());
            String addr = row.getCell(10).getStringCellValue();
            if (addr != null && !addr.isEmpty()) cust.setReceiveAddress(addr);
            list.add(cust);
        }
        return list;
    }

    private static List<Goods> loadGoods(Workbook workbook) {
        List<Goods> list = new ArrayList<>();
        Sheet sheet = workbook.getSheet("商品");
        if (sheet == null) return list;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            list.add(new Goods(
                    (int) row.getCell(0).getNumericCellValue(),
                    row.getCell(1).getStringCellValue(),
                    row.getCell(2).getStringCellValue(),
                    row.getCell(3).getStringCellValue(),
                    row.getCell(4).getStringCellValue(),
                    row.getCell(5).getNumericCellValue(),
                    row.getCell(6).getNumericCellValue(),
                    (int) row.getCell(7).getNumericCellValue()
            ));
        }
        return list;
    }

    // 加载订单并关联到对应顾客和商品
    private static void loadOrders(Workbook workbook, List<Customer> customers, List<Goods> goods) {
        Sheet sheet = workbook.getSheet("订单");
        if (sheet == null) return;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String orderNo = row.getCell(0).getStringCellValue();
            int customerId = (int) row.getCell(1).getNumericCellValue();
            LocalDateTime createTime = LocalDateTime.parse(row.getCell(2).getStringCellValue(), FORMATTER);
            double totalAmount = row.getCell(3).getNumericCellValue();
            String payMode = row.getCell(4).getStringCellValue();
            String itemStr = row.getCell(5).getStringCellValue();

            // 解析商品明细，匹配对应的商品对象
            List<CartItem> itemList = new ArrayList<>();
            String[] items = itemStr.split(",");
            for (String item : items) {
                String[] parts = item.split(":");
                int goodsId = Integer.parseInt(parts[0]);
                int num = Integer.parseInt(parts[1]);
                for (Goods g : goods) {
                    if (g.getGoodsId() == goodsId) {
                        itemList.add(new CartItem(g, num));
                        break;
                    }
                }
            }

            Order order = new Order(orderNo, createTime, itemList, totalAmount, payMode);
            // 关联到对应用户的订单列表
            for (Customer cust : customers) {
                if (cust.getId() == customerId) {
                    cust.getOrderList().add(order);
                    break;
                }
            }
        }
    }

    // 数据封装类，统一返回三类核心数据
    public static class DataPack {
        public List<Administrator> admins;
        public List<Customer> customers;
        public List<Goods> goods;

        public DataPack(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
            this.admins = admins;
            this.customers = customers;
            this.goods = goods;
        }
    }
}
