package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.spawner.SpawnerModuleType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * 槽位提示管理器
 * 管理每个模块类型对应的提示物品
 */
@OnlyIn(Dist.CLIENT)
public class SlotHintManager {
    
    private static final Map<SpawnerModuleType, ItemStack> HINT_ITEMS = new HashMap<>();
    
    /**
     * 初始化提示物品映射
     */
    public static void initializeHints() {
        // 为每种模块类型设置对应的提示物品
        HINT_ITEMS.put(SpawnerModuleType.RANGE_REDUCER, 
            new ItemStack(ExampleMod.RANGE_REDUCER_MODULE.get()));
        
        HINT_ITEMS.put(SpawnerModuleType.RANGE_EXPANDER, 
            new ItemStack(ExampleMod.RANGE_EXPANDER_MODULE.get()));
        
        HINT_ITEMS.put(SpawnerModuleType.MIN_DELAY_REDUCER, 
            new ItemStack(ExampleMod.MIN_DELAY_REDUCER_MODULE.get()));
        
        HINT_ITEMS.put(SpawnerModuleType.MAX_DELAY_REDUCER, 
            new ItemStack(ExampleMod.MAX_DELAY_REDUCER_MODULE.get()));
        
        HINT_ITEMS.put(SpawnerModuleType.COUNT_BOOSTER, 
            new ItemStack(ExampleMod.COUNT_BOOSTER_MODULE.get()));
        
        HINT_ITEMS.put(SpawnerModuleType.PLAYER_IGNORER, 
            new ItemStack(ExampleMod.PLAYER_IGNORER_MODULE.get()));
    }
    
    /**
     * 获取指定模块类型的提示物品
     * 
     * @param moduleType 模块类型
     * @return 对应的提示物品，如果没有则返回空物品
     */
    public static ItemStack getHintItem(SpawnerModuleType moduleType) {
        return HINT_ITEMS.getOrDefault(moduleType, ItemStack.EMPTY);
    }
    
    /**
     * 检查是否有指定模块类型的提示物品
     * 
     * @param moduleType 模块类型
     * @return 如果有提示物品返回true，否则返回false
     */
    public static boolean hasHintItem(SpawnerModuleType moduleType) {
        return HINT_ITEMS.containsKey(moduleType) && !HINT_ITEMS.get(moduleType).isEmpty();
    }
    
    /**
     * 清除所有提示物品映射
     */
    public static void clearHints() {
        HINT_ITEMS.clear();
    }
    
    /**
     * 添加或更新提示物品映射
     * 
     * @param moduleType 模块类型
     * @param hintItem 提示物品
     */
    public static void setHintItem(SpawnerModuleType moduleType, ItemStack hintItem) {
        HINT_ITEMS.put(moduleType, hintItem.copy());
    }
    
    /**
     * 移除指定模块类型的提示物品
     * 
     * @param moduleType 模块类型
     */
    public static void removeHintItem(SpawnerModuleType moduleType) {
        HINT_ITEMS.remove(moduleType);
    }
    
    /**
     * 获取所有已注册的模块类型
     * 
     * @return 所有已注册的模块类型
     */
    public static SpawnerModuleType[] getRegisteredModuleTypes() {
        return HINT_ITEMS.keySet().toArray(new SpawnerModuleType[0]);
    }
}
