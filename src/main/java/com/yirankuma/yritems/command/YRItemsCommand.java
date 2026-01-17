package com.yirankuma.yritems.command;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.utils.TextFormat;
import com.yirankuma.yritems.ItemData;
import com.yirankuma.yritems.YRItems;

import java.util.*;

public class YRItemsCommand extends Command {
    private final YRItems plugin;
    private static final int ITEMS_PER_PAGE = 10;
    
    public YRItemsCommand(YRItems plugin) {
        super("yritems", "YRItems插件命令", "/yritems <list|get|give|tags|nbt> [参数]", new String[]{"yri"});
        this.plugin = plugin;
        this.setPermission("yritems.command");
    }
    
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return false;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return handleListCommand(sender, args);
            case "get":
                return handleGetCommand(sender, args);
            case "give":
                return handleGiveCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            case "tags":
            case "nbt":
                return handleTagsCommand(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextFormat.YELLOW + "=== YRItems 命令帮助 ===");
        sender.sendMessage(TextFormat.GREEN + "/yritems list [页码] " + TextFormat.WHITE + "- 显示所有物品列表");
        sender.sendMessage(TextFormat.GREEN + "/yritems get <物品ID> " + TextFormat.WHITE + "- 获取指定物品");
        sender.sendMessage(TextFormat.GREEN + "/yritems give <玩家> <物品ID> [数量] " + TextFormat.WHITE + "- 给予玩家物品");
        sender.sendMessage(TextFormat.GREEN + "/yritems tags/nbt " + TextFormat.WHITE + "- 显示手持物品的NBT数据");
        sender.sendMessage(TextFormat.GREEN + "/yritems reload " + TextFormat.WHITE + "- 重载配置文件");
    }
    
    private boolean handleListCommand(CommandSender sender, String[] args) {
        Set<String> itemKeys = plugin.getItemConfig().getItemKeys();
        
        if (itemKeys.isEmpty()) {
            sender.sendMessage(TextFormat.RED + "没有找到任何物品配置！");
            return true;
        }
        
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(TextFormat.RED + "页码必须是数字！");
                return true;
            }
        }
        
        List<String> itemList = new ArrayList<>(itemKeys);
        int totalPages = (int) Math.ceil((double) itemList.size() / ITEMS_PER_PAGE);
        
        if (page < 1 || page > totalPages) {
            sender.sendMessage(TextFormat.RED + "页码超出范围！总共 " + totalPages + " 页");
            return true;
        }
        
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, itemList.size());
        
        sender.sendMessage(TextFormat.YELLOW + "=== 物品列表 (第 " + page + "/" + totalPages + " 页) ===");
        
        for (int i = startIndex; i < endIndex; i++) {
            String itemKey = itemList.get(i);
            ItemData itemData = plugin.getItemConfig().getItem(itemKey);
            sender.sendMessage(TextFormat.GREEN + "[" + (i + 1) + "] " + TextFormat.WHITE + itemKey + 
                             TextFormat.GRAY + " - " + TextFormat.RESET + itemData.getName());
        }
        
        if (page < totalPages) {
            sender.sendMessage(TextFormat.AQUA + "使用 /yritems list " + (page + 1) + " 查看下一页");
        }
        
        return true;
    }
    
    private boolean handleGetCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.RED + "此命令只能由玩家执行！");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED + "用法: /yritems get <物品ID>");
            return true;
        }
        
        String itemId = args[1];
        ItemData itemData = plugin.getItemConfig().getItem(itemId);
        
        if (itemData == null) {
            sender.sendMessage(TextFormat.RED + "物品 '" + itemId + "' 不存在！");
            return true;
        }
        
        Player player = (Player) sender;
        Item item = itemData.toItem(); // 直接使用ItemData的toItem方法

        if (player.getInventory().canAddItem(item)) {
            player.getInventory().addItem(item);
            // 从实际的Item对象获取已替换占位符后的名称
            String displayName = item.hasCustomName() ? item.getCustomName() : item.getName();
            sender.sendMessage(TextFormat.GREEN + "成功获得物品: " + TextFormat.RESET + displayName);
        } else {
            sender.sendMessage(TextFormat.RED + "背包空间不足！");
        }
        
        return true;
    }
    
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TextFormat.RED + "用法: /yritems give <玩家> <物品ID> [数量]");
            return true;
        }
        
        String playerName = args[1];
        String itemId = args[2];
        int amount = 1;
        
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    sender.sendMessage(TextFormat.RED + "数量必须大于0！");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(TextFormat.RED + "数量必须是数字！");
                return true;
            }
        }
        
        Player target = Server.getInstance().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "玩家 '" + playerName + "' 不在线！");
            return true;
        }
        
        ItemData itemData = plugin.getItemConfig().getItem(itemId);
        if (itemData == null) {
            sender.sendMessage(TextFormat.RED + "物品 '" + itemId + "' 不存在！");
            return true;
        }
        
        Item item = itemData.toItem(amount); // 直接使用ItemData的toItem方法并指定数量

        if (target.getInventory().canAddItem(item)) {
            target.getInventory().addItem(item);
            // 从实际的Item对象获取已替换占位符后的名称
            String displayName = item.hasCustomName() ? item.getCustomName() : item.getName();
            sender.sendMessage(TextFormat.GREEN + "成功给予 " + target.getName() + " " + amount + "个 " + displayName);
            target.sendMessage(TextFormat.GREEN + "你收到了 " + amount + "个 " + TextFormat.RESET + displayName);
        } else {
            sender.sendMessage(TextFormat.RED + "玩家 " + target.getName() + " 的背包空间不足！");
        }
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        sender.sendMessage(TextFormat.YELLOW + "正在重载YRItems插件...");
        plugin.reload();
        sender.sendMessage(TextFormat.GREEN + "✓ 配置文件已重载");
        sender.sendMessage(TextFormat.GREEN + "✓ 脚本已重载");
        sender.sendMessage(TextFormat.GREEN + "✓ 显示规则已重载");
        sender.sendMessage(TextFormat.GREEN + "重载完成！");
        return true;
    }
    
    private boolean handleTagsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.RED + "此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        // 不使用 getItemInHand()（有缓存问题），直接从槽位读取
        int heldSlot = player.getInventory().getHeldItemIndex();
        Item item = player.getInventory().getItem(heldSlot);

        if (item == null || item.getId() == 0) {
            sender.sendMessage(TextFormat.RED + "请手持一个物品！");
            return true;
        }
        
        sender.sendMessage(TextFormat.YELLOW + "=== 物品NBT数据 ===");
        sender.sendMessage(TextFormat.AQUA + "物品: " + TextFormat.WHITE + item.getName());
        sender.sendMessage(TextFormat.AQUA + "ID: " + TextFormat.WHITE + item.getId() + ":" + item.getDamage());
        
        if (item.hasCustomName()) {
            sender.sendMessage(TextFormat.AQUA + "自定义名称: " + TextFormat.WHITE + item.getCustomName());
        }
        
        String[] lore = item.getLore();
        if (lore != null && lore.length > 0) {
            sender.sendMessage(TextFormat.AQUA + "描述:");
            for (int i = 0; i < lore.length; i++) {
                sender.sendMessage(TextFormat.WHITE + "  - " + lore[i]);
            }
        }
        
        CompoundTag namedTag = item.getNamedTag();
        if (namedTag != null && !namedTag.isEmpty()) {
            sender.sendMessage(TextFormat.AQUA + "NBT数据:");
            String nbtYaml = formatNbtAsYaml(namedTag, 1);
            for (String line : nbtYaml.split("\n")) {
                sender.sendMessage(TextFormat.WHITE + line);
            }
        } else {
            sender.sendMessage(TextFormat.GRAY + "该物品没有NBT数据");
        }
        
        return true;
    }
    
    /**
     * 将NBT数据格式化为YAML格式的字符串
     * @param tag NBT标签
     * @param indent 缩进级别
     * @return 格式化后的字符串
     */
    private String formatNbtAsYaml(CompoundTag tag, int indent) {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);
        
        Collection<Tag> allTags = tag.getAllTags();
        for (Tag nbtTag : allTags) {
            String key = nbtTag.getName();
            if (key == null) continue;
            
            sb.append(indentStr).append(key).append(": ");
            
            switch (nbtTag.getId()) {
                case Tag.TAG_Compound:
                    sb.append("\n");
                    sb.append(formatNbtAsYaml((CompoundTag) nbtTag, indent + 1));
                    break;
                case Tag.TAG_List:
                    sb.append("\n");
                    sb.append(formatListAsYaml((ListTag<?>) nbtTag, indent + 1));
                    break;
                case Tag.TAG_String:
                    sb.append(nbtTag.parseValue()).append("\n");
                    break;
                case Tag.TAG_Byte:
                case Tag.TAG_Short:
                case Tag.TAG_Int:
                case Tag.TAG_Long:
                case Tag.TAG_Float:
                case Tag.TAG_Double:
                    sb.append(nbtTag.parseValue()).append("\n");
                    break;
                default:
                    sb.append(nbtTag.parseValue()).append("\n");
                    break;
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 将ListTag格式化为YAML格式的字符串
     * @param listTag 列表标签
     * @param indent 缩进级别
     * @return 格式化后的字符串
     */
    @SuppressWarnings("unchecked")
    private String formatListAsYaml(ListTag<?> listTag, int indent) {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);
        
        // 修正：使用强制类型转换处理泛型问题
        List<?> list = listTag.getAll();
        for (Object obj : list) {
            if (!(obj instanceof Tag)) {
                continue;
            }
            Tag tag = (Tag) obj;

            sb.append(indentStr).append("- ");

            switch (tag.getId()) {
                case Tag.TAG_Compound:
                    sb.append("\n");
                    // 对于复合标签，需要特殊处理缩进
                    String compoundYaml = formatNbtAsYaml((CompoundTag) tag, indent + 1);
                    sb.append(compoundYaml);
                    break;
                case Tag.TAG_List:
                    sb.append("\n");
                    sb.append(formatListAsYaml((ListTag<?>) tag, indent + 1));
                    break;
                default:
                    sb.append(tag.parseValue()).append("\n");
                    break;
            }
        }
        
        return sb.toString();
    }
}