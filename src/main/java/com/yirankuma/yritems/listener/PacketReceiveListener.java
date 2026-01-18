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

        /// 检查 MobEquipmentPacket（槽位切换）
        if (packet instanceof MobEquipmentPacket) {
            MobEquipmentPacket mobPacket = (MobEquipmentPacket) packet;

            try {
                int slot = mobPacket.hotbarSlot;
                cn.nukkit.Player player = event.getPlayer();

                // 获取服务器端该槽位的实际物品
                Item serverItem = player.getInventory().getItem(slot);

                // 检查是否是动态Lore物品
                boolean useDynamicLore = serverItem != null &&
                        serverItem.hasCompoundTag() &&
                        serverItem.getNamedTag().contains("UseDynamicLore") &&
                        serverItem.getNamedTag().getByte("UseDynamicLore") == 1;

                if (useDynamicLore) {
                    // 关键步骤：用服务器端的物品（无动态Lore）替换客户端发来的物品（有动态Lore）
                    // 这样 MOT 核心的反作弊检测就会通过
                    mobPacket.item = serverItem;

                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().debug("[假Lore绕过] 已替换槽位 " + slot + " 的物品数据，绕过反作弊检测");
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().error("处理 MobEquipmentPacket 失败: " + e.getMessage());
                    e.printStackTrace();
                }
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
