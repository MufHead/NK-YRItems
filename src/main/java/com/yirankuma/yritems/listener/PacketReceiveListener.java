package com.yirankuma.yritems.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.InventoryTransactionPacket;
import cn.nukkit.network.protocol.MobEquipmentPacket;
import cn.nukkit.network.protocol.PlayerActionPacket;
import cn.nukkit.network.protocol.types.NetworkInventoryAction;
import com.yirankuma.yritems.YRItems;
import com.yirankuma.yritems.hook.BinaryStreamHook;

/**
 * 数据包接收监听器
 * 用于在MOT处理之前移除动态Lore标记，避免反作弊拦截
 * 同时监听玩家的槽位切换请求
 */
public class PacketReceiveListener implements Listener {

    private final YRItems plugin;

    public PacketReceiveListener(YRItems plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听数据包接收事件
     * 优先级设置为LOWEST，确保在MOT处理之前执行
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPacketReceive(DataPacketReceiveEvent event) {
        DataPacket packet = event.getPacket();

        // 检查 MobEquipmentPacket（槽位切换）
        if (packet instanceof MobEquipmentPacket) {
            MobEquipmentPacket mobPacket = (MobEquipmentPacket) packet;

            // 强制同步服务器端的手持槽位
            // 只对有 YRAttributes 的特殊物品才同步
            try {
                int slot = mobPacket.hotbarSlot;
                cn.nukkit.Player player = event.getPlayer();

                // 检查该槽位的物品是否有 YRAttributes（服务器端物品没有 _DynamicLore）
                Item slotItem = player.getInventory().getItem(slot);
                boolean hasYRAttributes = slotItem != null &&
                                         slotItem.hasCompoundTag() &&
                                         slotItem.getNamedTag().contains("YRAttributes");

                if (hasYRAttributes) {
                    // 直接设置 heldItemIndex
                    player.getInventory().setHeldItemIndex(slot);
                }
            } catch (Exception ignored) {
                // 忽略异常
            }
        }

        // 只处理InventoryTransactionPacket
        if (!(packet instanceof InventoryTransactionPacket)) {
            return;
        }

        InventoryTransactionPacket transactionPacket = (InventoryTransactionPacket) packet;

        try {
            // 获取所有物品操作
            NetworkInventoryAction[] actions = transactionPacket.actions;
            if (actions == null || actions.length == 0) {
                return;
            }

            // 处理每个操作中的物品
            // 注意：NetworkInventoryAction的字段可能因MOT版本而异
            // 这里使用反射或try-catch来兼容不同版本
            boolean anyModified = false;
            for (NetworkInventoryAction action : actions) {
                try {
                    // 尝试获取并处理oldItem字段
                    Item oldItem = action.oldItem;
                    if (oldItem != null) {
                        if (BinaryStreamHook.removeDynamicLoreIfMarked(oldItem)) {
                            anyModified = true;
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    // 尝试获取并处理newItem字段
                    Item newItem = action.newItem;
                    if (newItem != null) {
                        if (BinaryStreamHook.removeDynamicLoreIfMarked(newItem)) {
                            anyModified = true;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (anyModified && plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().debug("已清理InventoryTransactionPacket中的动态Lore数据");
            }

        } catch (Exception e) {
            plugin.getLogger().error("处理InventoryTransactionPacket失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
}
