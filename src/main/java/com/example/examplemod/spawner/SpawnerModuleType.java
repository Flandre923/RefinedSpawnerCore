package com.example.examplemod.spawner;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 刷怪器模块类型枚举
 * 定义了所有可用的刷怪器增强模块
 */
public enum SpawnerModuleType {
    // 范围相关模块
    RANGE_REDUCER("range_reducer", "Range Reducer", "Reduces spawn range"),
    RANGE_EXPANDER("range_expander", "Range Expander", "Increases spawn range"),

    // 延迟相关模块
    MIN_DELAY_REDUCER("min_delay_reducer", "Min Delay Reducer", "Reduces minimum spawn delay"),
    MAX_DELAY_REDUCER("max_delay_reducer", "Max Delay Reducer", "Reduces maximum spawn delay"),

    // 数量相关模块
    COUNT_BOOSTER("count_booster", "Count Booster", "Increases spawn count"),

    // 特殊模块
    PLAYER_IGNORER("player_ignorer", "Player Ignorer", "Completely ignores player distance check"),
    SIMULATION_UPGRADE("simulation_upgrade", "Simulation Upgrade", "Kills spawned mobs and inserts drops into nearby containers"),

    // 模拟升级专用模块（仅在有模拟升级时可用）
    LOOTING_UPGRADE("looting_upgrade", "Looting Upgrade", "Increases looting level by 1 (max 16)"),
    BEHEADING_UPGRADE("beheading_upgrade", "Beheading Upgrade", "Increases beheading level by 1 (max 16)");

    private final String id;
    private final String displayName;
    private final String description;

    SpawnerModuleType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getEffectValue() {
        // 动态获取配置文件中的最新数值
        return com.example.examplemod.spawner.SpawnerModuleConfig.getModuleEffectValue(this);
    }

    public Component getDisplayComponent() {
        return Component.literal(displayName);
    }

    public Component getDescriptionComponent() {
        return Component.literal(description);
    }

    /**
     * 根据物品堆栈获取模块类型
     */
    public static SpawnerModuleType fromItemStack(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // 检查是否是SpawnerModuleItem
        if (stack.getItem() instanceof com.example.examplemod.item.SpawnerModuleItem moduleItem) {
            SpawnerModuleType type = moduleItem.getModuleType();
            System.out.println("SpawnerModuleType: Identified " + stack + " as " + type.getDisplayName());
            return type;
        }

        System.out.println("SpawnerModuleType: Failed to identify " + stack + " as a module");
        return null;
    }

    /**
     * 检查物品是否是有效的模块
     */
    public static boolean isValidModule(ItemStack stack) {
        return fromItemStack(stack) != null;
    }
}
