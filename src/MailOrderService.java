import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MailOrderService {
    private final ShoppingSystem system;
    private final String imapHost;
    private final int imapPort;
    private final boolean sslEnable;
    private final String emailAccount;
    private final String authCode;
    private final long pollInterval;
    private volatile boolean running = true;

    public MailOrderService(ShoppingSystem system) {
        this.system = system;
        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) prop.load(is);
        } catch (IOException e) {
            LogUtil.error("读取邮件下单配置失败", e);
        }
        this.imapHost = prop.getProperty("mail.imap.host", "imap.qq.com");
        this.imapPort = Integer.parseInt(prop.getProperty("mail.imap.port", "993"));
        this.sslEnable = Boolean.parseBoolean(prop.getProperty("mail.imap.ssl.enable", "true"));
        this.emailAccount = prop.getProperty("mail.sender", "");
        this.authCode = prop.getProperty("mail.auth.code", "");
        this.pollInterval = Long.parseLong(prop.getProperty("mail.order.poll.interval", "30000"));
    }

    /**
     * 启动后台守护线程，开始轮询收件箱
     */
    public void start() {
        if (!testConnection()) {
            System.out.println("========================================================");
            System.out.println("【邮件下单功能未启动】IMAP邮箱认证失败！");
            System.out.println("请检查 config.properties 中的以下配置：");
            System.out.println("  1. mail.sender  是否为完整的QQ邮箱地址");
            System.out.println("  2. mail.auth.code  是否为QQ邮箱生成的16位授权码（不是QQ登录密码）");
            System.out.println("  3. QQ邮箱设置中是否已开启 IMAP/SMTP 服务");
            System.out.println("  4. 如多次失败，可能触发了登录频率限制，请等待30分钟后重试");
            System.out.println("提示：可运行 ImapTest 类单独测试邮箱连接");
            System.out.println("========================================================");
            LogUtil.warn("邮件下单服务未启动：IMAP认证失败");
            return;
        }

        Thread pollThread = new Thread(this::runPoll, "邮件下单轮询线程");
        pollThread.setDaemon(true);
        pollThread.start();
        LogUtil.info("邮件下单服务已启动，轮询间隔：" + pollInterval + "毫秒");
        System.out.println("【邮件下单功能已启动】向 " + emailAccount + " 发送邮件即可远程查询商品和下单");
        System.out.println("  - 查询商品：主题或正文写「商品」");
        System.out.println("  - 下单：主题或正文写「下单」，正文附商品ID和数量");
    }

    private boolean testConnection() {
        if (emailAccount == null || emailAccount.isEmpty()
                || authCode == null || authCode.isEmpty()) {
            return false;
        }
        try {
            Properties props = buildImapProperties();
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(emailAccount, authCode);
            store.close();
            return true;
        } catch (AuthenticationFailedException e) {
            LogUtil.error("IMAP认证失败：" + e.getMessage(), e);
            return false;
        } catch (MessagingException e) {
            LogUtil.error("IMAP连接异常：" + e.getMessage(), e);
            return false;
        }
    }

    private Properties buildImapProperties() {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", imapHost);
        props.setProperty("mail.imaps.port", String.valueOf(imapPort));
        props.setProperty("mail.imaps.ssl.enable", String.valueOf(sslEnable));
        props.setProperty("mail.imaps.ssl.trust", imapHost);
        props.setProperty("mail.imaps.connectiontimeout", "10000");
        props.setProperty("mail.imaps.timeout", "10000");
        return props;
    }

    /**
     * 轮询主循环（带失败退避）
     */
    private void runPoll() {
        int failCount = 0;
        long baseInterval = pollInterval;

        while (running) {
            try {
                Thread.sleep(baseInterval);
                checkAndProcessMails();
                failCount = 0;
                baseInterval = pollInterval;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                failCount++;
                LogUtil.error("邮件轮询失败，连续失败次数：" + failCount, e);
                if (failCount >= 5) {
                    baseInterval = 60 * 60 * 1000L;
                    LogUtil.warn("连续失败5次，暂停轮询1小时");
                } else if (failCount >= 3) {
                    baseInterval = 10 * 60 * 1000L;
                } else if (failCount >= 1) {
                    baseInterval = 60 * 1000L;
                }
            }
        }
    }

    /**
     * 连接收件箱，处理所有未读邮件
     */
    private void checkAndProcessMails() throws MessagingException {
        Properties props = buildImapProperties();
        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        try {
            store.connect(emailAccount, authCode);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            int unreadCount = 0;
            for (Message msg : messages) {
                if (msg.isSet(Flags.Flag.SEEN)) continue;
                unreadCount++;

                String subject = "";
                String fromEmail = "";
                String userContent = "";
                try {
                    subject = msg.getSubject() == null ? "" : msg.getSubject().trim();
                    fromEmail = msg.getFrom() != null && msg.getFrom().length > 0
                            ? ((InternetAddress) msg.getFrom()[0]).getAddress() : "未知";
                    // 关键：支持HTML邮件，提取纯文本正文
                    String rawContent = extractTextFromPart(msg);
                    userContent = extractUserContent(rawContent);
                } catch (Exception e) {
                    LogUtil.error("读取邮件内容失败，发件人：" + fromEmail + "，主题：" + subject, e);
                    msg.setFlag(Flags.Flag.SEEN, true);
                    continue;
                }

                // 关键：只处理系统已注册用户的邮件，未注册发件人直接跳过（过滤垃圾邮件）
                Customer registeredCustomer = findCustomerByEmail(fromEmail);
                if (registeredCustomer == null) {
                    System.out.println("【邮件轮询】未注册发件人，已忽略 | " + fromEmail + " | 主题：" + subject);
                    msg.setFlag(Flags.Flag.SEEN, true);
                    continue;
                }

                // 调试日志：只打印已注册用户的邮件
                System.out.println("【邮件轮询】收到注册用户邮件 | 顾客：" + registeredCustomer.getName()
                        + " | 发件人：" + fromEmail
                        + " | 主题：" + subject
                        + " | 正文摘要：" + truncate(userContent.replaceAll("\\s+", " "), 80));

                // 判断指令类型
                boolean isCatalog = isCatalogMail(subject) || startsWithCatalogCommand(userContent);
                boolean isOrder = isOrderMail(subject) || startsWithOrderCommand(userContent);

                if (isCatalog) {
                    try {
                        processCatalogMail(msg, fromEmail);
                    } catch (Exception e) {
                        LogUtil.error("处理商品目录查询失败", e);
                        msg.setFlag(Flags.Flag.SEEN, true);
                    }
                } else if (isOrder) {
                    try {
                        processSingleMail(msg, fromEmail, userContent);
                    } catch (Exception e) {
                        LogUtil.error("处理下单邮件失败", e);
                        msg.setFlag(Flags.Flag.SEEN, true);
                    }
                } else {
                    // 已注册用户的非指令邮件，标记已读
                    System.out.println("【邮件轮询】非指令邮件，已跳过 | 主题：" + subject);
                    msg.setFlag(Flags.Flag.SEEN, true);
                }
            }

            if (unreadCount > 0) {
                System.out.println("【邮件轮询】本轮处理了 " + unreadCount + " 封未读邮件");
            }
            inbox.close(false);
        } finally {
            store.close();
        }
    }

    /**
     * 处理下单邮件
     */
    private void processSingleMail(Message msg, String fromEmail, String content) throws Exception {
        Customer customer = findCustomerByEmail(fromEmail);

        if (customer == null) {
            replyMail(fromEmail, "【下单失败】身份验证失败",
                    "身份验证失败：该邮箱未在系统注册账号。\n请先在购物系统中注册顾客账号，注册邮箱需与发件邮箱一致。");
            msg.setFlag(Flags.Flag.SEEN, true);
            return;
        }

        Map<Integer, Integer> orderItems = parseOrderContent(content);

        if (orderItems.isEmpty()) {
            replyMail(fromEmail, "【下单失败】订单格式错误",
                    "订单格式错误。\n\n正确格式：\n"
                            + "主题或正文第一行写「下单」\n"
                            + "正文每行一个商品：商品ID 数量\n\n"
                            + "示例：\n"
                            + "  下单\n"
                            + "  101 2\n"
                            + "  102 1\n\n"
                            + "如需查看商品ID，请回复「商品」获取商品目录。");
            msg.setFlag(Flags.Flag.SEEN, true);
            return;
        }

        // 校验商品与库存
        StringBuilder errorMsg = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            int goodsId = entry.getKey();
            int num = entry.getValue();
            Goods goods = findGoodsById(goodsId);
            if (goods == null) {
                errorMsg.append("商品ID ").append(goodsId).append(" 不存在\n");
            } else if (goods.getStock() < num) {
                errorMsg.append("商品[").append(goods.getGoodsName()).append("]库存不足，剩余").append(goods.getStock()).append("件\n");
            }
        }

        if (errorMsg.length() > 0) {
            replyMail(fromEmail, "【下单失败】商品校验未通过", errorMsg.toString());
            msg.setFlag(Flags.Flag.SEEN, true);
            return;
        }

        // 执行下单
        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            customer.addCart(entry.getKey(), entry.getValue(), system.getGoodsRepository());
        }
        boolean success = customer.checkout(system.getGoodsRepository(), "邮件下单");

        if (success) {
            Order latestOrder = customer.getOrderList().get(customer.getOrderList().size() - 1);
            system.saveDataInternal();

            // 控制台输出
            System.out.println("========================================================");
            System.out.println("【邮件下单成功】订单已自动更新并存储");
            System.out.println("  顾客：" + customer.getName() + "（ID:" + customer.getId() + "）");
            System.out.println("  订单号：" + latestOrder.getOrderNo());
            System.out.println("  下单时间：" + latestOrder.getCreateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("  商品明细：");
            for (CartItem item : latestOrder.getItemList()) {
                System.out.println("    - " + item.getGoods().getGoodsName()
                        + " × " + item.getBuyNumber()
                        + " 小计：" + String.format("%.2f", item.getSubtotal()) + "元");
            }
            System.out.println("  订单总金额：" + String.format("%.2f", latestOrder.getTotalAmount()) + "元");
            System.out.println("  顾客累计消费：" + String.format("%.2f", customer.getTotalConsume()) + "元");
            System.out.println("  数据已持久化保存到：" + system.getPersistenceType());
            System.out.println("========================================================");

            // 回复成功邮件
            StringBuilder successMsg = new StringBuilder();
            successMsg.append("下单成功！\n\n");
            successMsg.append("订单号：").append(latestOrder.getOrderNo()).append("\n");
            successMsg.append("下单时间：").append(latestOrder.getCreateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            successMsg.append("商品明细：\n");
            for (CartItem item : latestOrder.getItemList()) {
                successMsg.append("- ").append(item.getGoods().getGoodsName())
                        .append(" × ").append(item.getBuyNumber())
                        .append("  单价：").append(String.format("%.2f", item.getGoods().getRetailPrice())).append("元")
                        .append("  小计：").append(String.format("%.2f", item.getSubtotal())).append("元\n");
            }
            successMsg.append("\n订单总金额：").append(String.format("%.2f", latestOrder.getTotalAmount())).append("元");

            replyMail(fromEmail, "【下单成功】订单通知", successMsg.toString());
            LogUtil.info("邮件下单成功，顾客：" + customer.getName() + "，订单号：" + latestOrder.getOrderNo());
        } else {
            replyMail(fromEmail, "【下单失败】系统错误", "下单失败，请稍后重试。");
        }

        msg.setFlag(Flags.Flag.SEEN, true);
    }

    /**
     * 处理商品目录查询邮件
     */
    private void processCatalogMail(Message msg, String fromEmail) throws Exception {
        List<Goods> goodsList = system.getGoodsRepository();

        StringBuilder sb = new StringBuilder();
        sb.append("===== 商品目录 =====\n\n");
        if (goodsList.isEmpty()) {
            sb.append("暂无商品，请稍后再试。\n");
        } else {
            for (Goods g : goodsList) {
                sb.append("【商品ID】").append(g.getGoodsId()).append("\n");
                sb.append("  名称：").append(g.getGoodsName()).append("\n");
                sb.append("  厂家：").append(g.getManufacturer()).append("\n");
                sb.append("  型号：").append(g.getModel()).append("\n");
                sb.append("  零售价：").append(String.format("%.2f", g.getRetailPrice())).append("元\n");
                sb.append("  库存：").append(g.getStock()).append("件\n");
                sb.append("\n");
            }
            sb.append("====================\n");
            sb.append("下单方式：回复本邮件，正文写：\n");
            sb.append("  下单\n");
            sb.append("  商品ID 数量\n");
            sb.append("示例：\n");
            sb.append("  下单\n");
            sb.append("  101 1\n");
            sb.append("  102 2\n");
        }

        MailUtil.sendMail(fromEmail, "【商品目录】当前可购买商品清单", sb.toString());
        LogUtil.info("商品目录已发送至：" + fromEmail);
        System.out.println("【邮件查询】已向 " + fromEmail + " 发送商品目录");
        msg.setFlag(Flags.Flag.SEEN, true);
    }

    // ========== 指令识别方法 ==========

    private boolean isOrderMail(String subject) {
        if (subject == null || subject.isBlank()) return false;
        return subject.contains("下单");
    }

    private boolean startsWithOrderCommand(String content) {
        if (content == null || content.isBlank()) return false;
        Scanner scanner = new Scanner(content);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;
            boolean result = "下单".equalsIgnoreCase(line) || line.startsWith("下单");
            scanner.close();
            return result;
        }
        scanner.close();
        return false;
    }

    private boolean isCatalogMail(String subject) {
        if (subject == null || subject.isBlank()) return false;
        String lower = subject.toLowerCase();
        return lower.contains("商品") || lower.contains("目录")
                || lower.contains("catalog") || lower.contains("goods")
                || lower.contains("列表");
    }

    private boolean startsWithCatalogCommand(String content) {
        if (content == null || content.isBlank()) return false;
        Scanner scanner = new Scanner(content);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;
            String lower = line.toLowerCase();
            boolean result = lower.equals("商品") || lower.equals("目录")
                    || lower.equals("catalog") || lower.equals("goods")
                    || lower.equals("列表") || lower.equals("商品列表");
            scanner.close();
            return result;
        }
        scanner.close();
        return false;
    }

    // ========== 邮件内容提取（核心修复：支持HTML邮件） ==========

    /**
     * 从邮件Part中提取纯文本，支持text/plain、text/html、multipart
     * 这是修复的核心：QQ邮箱手机APP发的是HTML邮件，之前只处理text/plain导致正文为空
     */
    private String extractTextFromPart(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            return content == null ? "" : content.toString();
        } else if (part.isMimeType("text/html")) {
            // 关键修复：HTML邮件转纯文本
            Object content = part.getContent();
            String html = content == null ? "" : content.toString();
            return htmlToPlainText(html);
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            // 优先找text/plain，找不到再找text/html
            String htmlText = "";
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    String text = extractTextFromPart(bp);
                    if (text != null && !text.isBlank()) return text;
                } else if (bp.isMimeType("text/html")) {
                    if (htmlText.isEmpty()) {
                        htmlText = extractTextFromPart(bp);
                    }
                } else if (bp.getContent() instanceof Multipart) {
                    String text = extractTextFromPart(bp);
                    if (text != null && !text.isBlank()) return text;
                }
            }
            return htmlText;
        }
        return "";
    }

    /**
     * 简单的HTML转纯文本：去除标签，转换换行
     */
    private String htmlToPlainText(String html) {
        if (html == null || html.isBlank()) return "";
        String text = html;
        // 块级标签转换行
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p\\s*>", "\n");
        text = text.replaceAll("(?i)</div\\s*>", "\n");
        text = text.replaceAll("(?i)</tr\\s*>", "\n");
        text = text.replaceAll("(?i)</li\\s*>", "\n");
        // 去除所有HTML标签
        text = text.replaceAll("<[^>]+>", "");
        // 解码常见HTML实体
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&#39;", "'");
        // 去除多余空行
        text = text.replaceAll("\\n{3,}", "\n\n");
        // 去除每行首尾空格
        StringBuilder result = new StringBuilder();
        Scanner scanner = new Scanner(text);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                result.append(line).append("\n");
            }
        }
        scanner.close();
        return result.toString().trim();
    }

    /**
     * 从邮件正文中提取用户实际输入的部分，过滤回复引用内容
     */
    private String extractUserContent(String fullContent) {
        if (fullContent == null || fullContent.isBlank()) return "";
        String[] quoteMarkers = {
                "---原始邮件---",
                "----- 原始邮件 -----",
                "-----Original Message-----",
                "--- Original Message ---",
                "----- 原始邮件-----",
                "---原始邮件 ----",
                "在 ",
                "On ",
                "发件人:",
                "From:",
                "> "
        };
        for (String marker : quoteMarkers) {
            int idx = fullContent.indexOf(marker);
            if (idx >= 0) {
                return fullContent.substring(0, idx);
            }
        }
        return fullContent;
    }

    /**
     * 解析邮件正文，返回 商品ID -> 购买数量
     */
    private Map<Integer, Integer> parseOrderContent(String content) {
        Map<Integer, Integer> items = new HashMap<>();
        if (content == null || content.isBlank()) return items;

        Scanner scanner = new Scanner(content);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;
            // 跳过"下单"指令行
            if ("下单".equalsIgnoreCase(line) || line.startsWith("下单")) continue;
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                try {
                    int id = Integer.parseInt(parts[0]);
                    int num = Integer.parseInt(parts[1]);
                    if (id > 0 && num > 0) {
                        items.put(id, num);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        scanner.close();
        return items;
    }

    // ========== 工具方法 ==========

    private Customer findCustomerByEmail(String email) {
        for (Customer c : system.getCustomerList()) {
            if (c.getEmail().equalsIgnoreCase(email)) {
                return c;
            }
        }
        return null;
    }

    private Goods findGoodsById(int goodsId) {
        for (Goods g : system.getGoodsRepository()) {
            if (g.getGoodsId() == goodsId) {
                return g;
            }
        }
        return null;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }

    private void replyMail(String toEmail, String subject, String content) {
        MailUtil.sendMail(toEmail, subject, content);
    }
}
