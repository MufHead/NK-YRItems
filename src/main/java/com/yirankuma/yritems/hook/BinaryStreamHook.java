package com.yirankuma.yritems.hook;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import com.yirankuma.yritems.YRItems;

/**
 * 动态Lore反作弊兼容工具
 *
 * 工作原理：
 * 1. 发送给客户端：在物品NBT中添加特殊标记 "_DynamicLore: 1b"
 * 2. 客户端操作物品后发送InventoryTransactionPacket
 * 3. 接收数据包：在 DataPacketReceiveEvent 中拦截，检测标记
 * 4. 如果检测到标记，移除 Lore 和标记本身
 * 5. MOT验证时物品NBT与服务器原始数据一致，验证通过
 *
 * 关键：这个处理必须在 MOT 的 BinaryStream.getSlot() 之前完成
 */
public class BinaryStreamHook {

    /** 动态Lore标记键 */
    public static final String DYNAMIC_LORE_MARKER = "_DynamicLore";

    private static YRItems plugin;

    /**
     * 初始化
     */
    public static void init(YRItems plugin) {
        BinaryStreamHook.plugin = plugin;
        plugin.getLogger().info("动态Lore反作弊兼容已启用");
    }

    /**
     * 检查物品是否应该添加动态Lore标记
     */
    public static boolean shouldAddDynamicLoreMarker(Item item) {
        if (item == null || item.getId() == 0) {
            return false;
        }

        CompoundTag nbt = item.getNamedTag();
        if (nbt == null || nbt.isEmpty()) {
            return false;
        }

        // 检查是否有YRAttributes
        return nbt.contains("YRAttributes");
    }

    /**
     * 给物品添加动态Lore标记
     * 在发送数据包给客户端之前调用
     */
    public static void addDynamicLoreMarker(Item item) {
        CompoundTag nbt = item.getNamedTag();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 添加标记（使用byte类型，值为1）
        nbt.putByte(DYNAMIC_LORE_MARKER, 1);
        item.setNamedTag(nbt);
    }

    /**
     * 移除物品的动态Lore和标记
     * 在接收客户端数据包时调用
     *
     * @param item 物品
     * @return 是否进行了处理
     */
    public static boolean removeDynamicLoreIfMarked(Item item) {
        if (item == null || item.getId() == 0) {
            return false;
        }

        // 检查物品NBT
        CompoundTag nbt = item.getNamedTag();
        if (nbt == null || nbt.isEmpty()) {
            return false;
        }

        // 检查是否有动态Lore标记
        if (!nbt.contains(DYNAMIC_LORE_MARKER)) {
            return false;
        }

        // 检查调用堆栈，只在处理数据包时才清理
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        boolean isFromPacket = false;

        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            // 检查是否来自数据包处理
            if (className.contains("DataPacket") ||
                className.contains("InventoryTransaction") ||
                className.contains("NetworkInventoryAction") ||
                className.contains("PacketReceiveListener")) {
                isFromPacket = true;
                break;
            }
        }

        // 如果不是来自数据包处理，不要清理
        if (!isFromPacket) {
            return false;
        }

        // 移除Lore
        boolean modified = false;
        if (nbt.contains("display")) {
            CompoundTag display = nbt.getCompound("display");
            if (display.contains("Lore")) {
                display.remove("Lore");
                modified = true;

                // 如果display标签现在为空，删除它
                if (display.isEmpty()) {
                    nbt.remove("display");
                }
            }
        }

        // 移除标记本身
        nbt.remove(DYNAMIC_LORE_MARKER);

        // 更新物品NBT
        if (modified || nbt.contains(DYNAMIC_LORE_MARKER)) {
            item.setNamedTag(nbt);
        }

        return modified;
    }
}
