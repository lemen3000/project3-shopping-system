import org.apache.poi.xssf.usermodel.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExcelPersistence implements Persistence {
    private static final String FILE_NAME = "shop_data.xlsx";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void saveAll(List<Administrator> admins, List<Customer> customers, List<Goods> goods) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        // 管理员工作表
        XSSFSheet adminSheet = workbook.createSheet("管理员");
        String[] adminHeaders = {"ID", "用户名", "密码", "手机号", "邮箱", "注册时间", "错误次数", "锁定状态"};
        XSSFRow adminHeaderRow = adminSheet.createRow(0);
        for (int i = 0; i < adminHeaders.length; i++) {
            adminHeaderRow.createCell(i).setCellValue(adminHeaders[i]);
        }
        int rowIndex = 1;
        for (Administrator admin : admins) {
            XSSFRow row = adminSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(admin.getId());
            row.createCell(1).setCellValue(admin.getName());
            row.createCell(2).setCellValue(admin.getPassword());
            row.createCell(3).setCellValue(admin.getPhone());
            row.createCell(4).setCellValue(admin.getEmail());
            row.createCell(5).setCellValue(admin.getRegisterTime().format(FORMATTER));
            row.createCell(6).setCellValue(admin.getErrorCount());
            row.createCell(7).setCellValue(admin.isLock() ? 1 : 0);
        }

        // 顾客工作表
        XSSFSheet custSheet = workbook.createSheet("顾客");
        String[] custHeaders = {"ID", "用户名", "密码", "手机号", "邮箱", "注册时间", "错误次数", "锁定状态", "等级", "累计消费", "收货地址"};
        XSSFRow custHeaderRow = custSheet.createRow(0);
        for (int i = 0; i < custHeaders.length; i++) {
            custHeaderRow.createCell(i).setCellValue(custHeaders[i]);
        }
        rowIndex = 1;
        for (Customer cust : customers) {
            XSSFRow row = custSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cust.getId());
            row.createCell(1).setCellValue(cust.getName());
            row.createCell(2).setCellValue(cust.getPassword());
            row.createCell(3).setCellValue(cust.getPhone());
            row.createCell(4).setCellValue(cust.getEmail());
            row.createCell(5).setCellValue(cust.getRegisterTime().format(FORMATTER));
            row.createCell(6).setCellValue(cust.getErrorCount());
            row.createCell(7).setCellValue(cust.isLock() ? 1 : 0);
            row.createCell(8).setCellValue(cust.getCustomerRank());
            row.createCell(9).setCellValue(cust.getTotalConsume());
            row.createCell(10).setCellValue(cust.getReceiveAddress() == null ? "" : cust.getReceiveAddress());
        }

        // 商品工作表
        XSSFSheet goodsSheet = workbook.createSheet("商品");
        String[] goodsHeaders = {"商品ID", "商品名称", "生产厂家", "生产日期", "型号", "进货价", "零售价", "库存"};
        XSSFRow goodsHeaderRow = goodsSheet.createRow(0);
        for (int i = 0; i < goodsHeaders.length; i++) {
            goodsHeaderRow.createCell(i).setCellValue(goodsHeaders[i]);
        }
        rowIndex = 1;
        for (Goods g : goods) {
            XSSFRow row = goodsSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(g.getGoodsId());
            row.createCell(1).setCellValue(g.getGoodsName());
            row.createCell(2).setCellValue(g.getManufacturer());
            row.createCell(3).setCellValue(g.getProduceDate());
            row.createCell(4).setCellValue(g.getModel());
            row.createCell(5).setCellValue(g.getPurchasePrice());
            row.createCell(6).setCellValue(g.getRetailPrice());
            row.createCell(7).setCellValue(g.getStock());
        }

        // 订单工作表
        XSSFSheet orderSheet = workbook.createSheet("订单");
        String[] orderHeaders = {"订单号", "顾客ID", "下单时间", "总金额", "支付方式", "商品明细"};
        XSSFRow orderHeaderRow = orderSheet.createRow(0);
        for (int i = 0; i < orderHeaders.length; i++) {
            orderHeaderRow.createCell(i).setCellValue(orderHeaders[i]);
        }
        rowIndex = 1;
        for (Customer cust : customers) {
            for (Order order : cust.getOrderList()) {
                XSSFRow row = orderSheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(order.getOrderNo());
                row.createCell(1).setCellValue(cust.getId());
                row.createCell(2).setCellValue(order.getCreateTime().format(FORMATTER));
                row.createCell(3).setCellValue(order.getTotalAmount());
                row.createCell(4).setCellValue(order.getPayMode());
                StringBuilder sb = new StringBuilder();
                List<CartItem> items = order.getItemList();
                for (int i = 0; i < items.size(); i++) {
                    CartItem item = items.get(i);
                    sb.append(item.getGoods().getGoodsId()).append(":").append(item.getBuyNumber());
                    if (i < items.size() - 1) sb.append(",");
                }
                row.createCell(5).setCellValue(sb.toString());
            }
        }

        try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
            workbook.write(fos);
        } catch (IOException e) {
            LogUtil.error("保存Excel文件失败", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public DataPack loadAll() {
        List<Administrator> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();

        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return new DataPack(admins, customers, goods);
        }

        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            // 加载管理员
            XSSFSheet adminSheet = workbook.getSheet("管理员");
            if (adminSheet != null) {
                for (int i = 1; i <= adminSheet.getLastRowNum(); i++) {
                    XSSFRow row = adminSheet.getRow(i);
                    if (row == null) continue;
                    Administrator admin = new Administrator(
                            (int) row.getCell(0).getNumericCellValue(),
                            row.getCell(1).getStringCellValue(),
                            row.getCell(2).getStringCellValue(),
                            row.getCell(3).getStringCellValue(),
                            row.getCell(4).getStringCellValue(),
                            LocalDateTime.parse(row.getCell(5).getStringCellValue(), FORMATTER),
                            (int) row.getCell(6).getNumericCellValue(),
                            getBooleanCellValue(row.getCell(7))
                    );
                    admins.add(admin);
                }
            }

            // 加载商品
            XSSFSheet goodsSheet = workbook.getSheet("商品");
            if (goodsSheet != null) {
                for (int i = 1; i <= goodsSheet.getLastRowNum(); i++) {
                    XSSFRow row = goodsSheet.getRow(i);
                    if (row == null) continue;
                    Goods g = new Goods(
                            (int) row.getCell(0).getNumericCellValue(),
                            row.getCell(1).getStringCellValue(),
                            row.getCell(2).getStringCellValue(),
                            row.getCell(3).getStringCellValue(),
                            row.getCell(4).getStringCellValue(),
                            row.getCell(5).getNumericCellValue(),
                            row.getCell(6).getNumericCellValue(),
                            (int) row.getCell(7).getNumericCellValue()
                    );
                    goods.add(g);
                }
            }

            // 加载顾客
            XSSFSheet custSheet = workbook.getSheet("顾客");
            if (custSheet != null) {
                for (int i = 1; i <= custSheet.getLastRowNum(); i++) {
                    XSSFRow row = custSheet.getRow(i);
                    if (row == null) continue;
                    Customer cust = new Customer(
                            (int) row.getCell(0).getNumericCellValue(),
                            row.getCell(1).getStringCellValue(),
                            row.getCell(2).getStringCellValue(),
                            row.getCell(3).getStringCellValue(),
                            row.getCell(4).getStringCellValue(),
                            LocalDateTime.parse(row.getCell(5).getStringCellValue(), FORMATTER),
                            (int) row.getCell(6).getNumericCellValue(),
                            getBooleanCellValue(row.getCell(7))
                    );
                    cust.setCustomerRank((int) row.getCell(8).getNumericCellValue());
                    cust.setTotalConsume(row.getCell(9).getNumericCellValue());
                    cust.setReceiveAddress(row.getCell(10).getStringCellValue());
                    customers.add(cust);
                }
            }

            // 加载订单
            XSSFSheet orderSheet = workbook.getSheet("订单");
            if (orderSheet != null) {
                for (int i = 1; i <= orderSheet.getLastRowNum(); i++) {
                    XSSFRow row = orderSheet.getRow(i);
                    if (row == null) continue;
                    int custId = (int) row.getCell(1).getNumericCellValue();
                    Customer owner = null;
                    for (Customer c : customers) {
                        if (c.getId() == custId) {
                            owner = c;
                            break;
                        }
                    }
                    if (owner == null) continue;

                    String detailStr = row.getCell(5).getStringCellValue();
                    List<CartItem> itemList = new ArrayList<>();
                    if (detailStr != null && !detailStr.isBlank()) {
                        String[] items = detailStr.split(",");
                        for (String item : items) {
                            String[] parts = item.split(":");
                            if (parts.length < 2) continue;
                            int goodsId = Integer.parseInt(parts[0]);
                            int num = Integer.parseInt(parts[1]);
                            Goods matchGoods = null;
                            for (Goods g : goods) {
                                if (g.getGoodsId() == goodsId) {
                                    matchGoods = g;
                                    break;
                                }
                            }
                            if (matchGoods != null) {
                                itemList.add(new CartItem(matchGoods, num));
                            }
                        }
                    }

                    Order order = new Order(
                            row.getCell(0).getStringCellValue(),
                            LocalDateTime.parse(row.getCell(2).getStringCellValue(), FORMATTER),
                            itemList,
                            row.getCell(3).getNumericCellValue(),
                            row.getCell(4).getStringCellValue()
                    );
                    owner.getOrderList().add(order);
                }
            }

        } catch (Exception e) {
            LogUtil.error("读取Excel文件失败", e);
        }

        return new DataPack(admins, customers, goods);
    }

    /**
     * 兼容读取布尔值：支持数字(1/0)、布尔(true/false)、字符串("true"/"1")
     */
    private boolean getBooleanCellValue(XSSFCell cell) {
        if (cell == null) {
            return false;
        }
        switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case NUMERIC:
                return cell.getNumericCellValue() == 1;
            case STRING:
                String str = cell.getStringCellValue().trim();
                return "true".equalsIgnoreCase(str) || "1".equals(str);
            default:
                return false;
        }
    }
}
