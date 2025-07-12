package com.example.examplemod.spawner;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 刷怪器模块类型枚举
 * 定义了所有可用的刷怪器增强模块
 */
public enum SpawnerModuleType {
    // 范围相关模块
    RANGE_REDUCER("range_reducer", "Range Reducer", "Reduces spawn range", -2),
    RANGE_EXPANDER("range_expander", "Range Expander", "Increases spawn range", 3),
    
    // 延迟相关模块
    MIN_DELAY_REDUCER("min_delay_reducer", "Min Delay Reducer", "Reduces minimum spawn delay", -50),
    MAX_DELAY_REDUCER("max_delay_reducer", "Max Delay Reducer", "Reduces maximum spawn delay", -100),
    
    // 数量相关模块
    COUNT_BOOSTER("count_booster", "Count Booster", "Increases spawn count", 2),
    
    // 特殊模块
    PLAYER_IGNORER("player_ignorer", "Player Ignorer", "Ignores player range requirement", 0);

    private final String id;
    private final String displayName;
    private final String description;
    private final int effectValue;

    SpawnerModuleType(String id, String displayName, String description, int effectValue) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.effectValue = effectValue;
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
        return effectValue;
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
