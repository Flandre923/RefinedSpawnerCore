package com.example.examplemod.spawner;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/**
 * 刷怪器模块管理器
 * 负责管理模块的安装、卸载和效果计算
 */
public class SpawnerModuleManager {

    private final NonNullList<ItemStack> moduleSlots;
    private final Map<SpawnerModuleType, Integer> moduleCount;
    private Runnable changeListener;
    
    public SpawnerModuleManager(int slotCount) {
        this.moduleSlots = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        this.moduleCount = new EnumMap<>(SpawnerModuleType.class);
    }

    /**
     * 设置变更监听器
     */
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }
    
    /**
     * 获取模块槽位
     */
    public NonNullList<ItemStack> getModuleSlots() {
        return moduleSlots;
    }
    
    /**
     * 设置指定槽位的模块
     */
    public void setModule(int slot, ItemStack stack) {
        if (slot >= 0 && slot < moduleSlots.size()) {
            moduleSlots.set(slot, stack);
            recalculateModules();
            System.out.println("SpawnerModuleManager: Set module at slot " + slot + " to " + stack + ", recalculated modules");
        }
    }
    
    /**
     * 获取指定槽位的模块
     */
    public ItemStack getModule(int slot) {
        if (slot >= 0 && slot < moduleSlots.size()) {
            return moduleSlots.get(slot);
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * 重新计算所有模块效果
     */
    public void recalculateModules() {
        moduleCount.clear();

        System.out.println("SpawnerModuleManager: Recalculating modules...");
        for (int i = 0; i < moduleSlots.size(); i++) {
            ItemStack stack = moduleSlots.get(i);
            if (!stack.isEmpty()) {
                SpawnerModuleType type = SpawnerModuleType.fromItemStack(stack);
                if (type != null) {
                    moduleCount.merge(type, stack.getCount(), Integer::sum);
                    System.out.println("  Slot " + i + ": " + type.getDisplayName() + " x" + stack.getCount());
                } else {
                    System.out.println("  Slot " + i + ": Invalid module " + stack);
                }
            }
        }

        System.out.println("SpawnerModuleManager: Final module counts: " + moduleCount);

        // 通知变更监听器
        if (changeListener != null) {
            changeListener.run();
        }

        // 通知客户端渲染器更新（如果在客户端）
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            notifyRenderUpdate();
        }
    }
    
    /**
     * 获取指定类型模块的数量
     */
    public int getModuleCount(SpawnerModuleType type) {
        return moduleCount.getOrDefault(type, 0);
    }
    
    /**
     * 检查是否有指定类型的模块
     */
    public boolean hasModule(SpawnerModuleType type) {
        return getModuleCount(type) > 0;
    }
    
    /**
     * 计算刷怪范围修正值
     */
    public int getSpawnRangeModifier() {
        int modifier = 0;
        modifier += getModuleCount(SpawnerModuleType.RANGE_REDUCER) * SpawnerModuleConfig.RANGE_REDUCER_VALUE;
        modifier += getModuleCount(SpawnerModuleType.RANGE_EXPANDER) * SpawnerModuleConfig.RANGE_EXPANDER_VALUE;
        return modifier;
    }
    
    /**
     * 计算最小刷怪延迟修正值
     */
    public int getMinSpawnDelayModifier() {
        return getModuleCount(SpawnerModuleType.MIN_DELAY_REDUCER) * SpawnerModuleConfig.MIN_DELAY_REDUCER_VALUE;
    }
    
    /**
     * 计算最大刷怪延迟修正值
     */
    public int getMaxSpawnDelayModifier() {
        return getModuleCount(SpawnerModuleType.MAX_DELAY_REDUCER) * SpawnerModuleConfig.MAX_DELAY_REDUCER_VALUE;
    }
    
    /**
     * 计算刷怪数量修正值
     */
    public int getSpawnCountModifier() {
        return getModuleCount(SpawnerModuleType.COUNT_BOOSTER) * SpawnerModuleConfig.COUNT_BOOSTER_VALUE;
    }
    
    /**
     * 检查是否忽略玩家距离
     */
    public boolean shouldIgnorePlayer() {
        return hasModule(SpawnerModuleType.PLAYER_IGNORER);
    }
    
    /**
     * 应用所有模块效果到基础值
     */
    public SpawnerStats applyModules(SpawnerStats baseStats) {
        int newSpawnRange = SpawnerModuleConfig.clampSpawnRange(
            baseStats.spawnRange() + getSpawnRangeModifier()
        );
        
        int newMinDelay = SpawnerModuleConfig.clampMinSpawnDelay(
            baseStats.minSpawnDelay() + getMinSpawnDelayModifier()
        );
        
        int newMaxDelay = SpawnerModuleConfig.clampMaxSpawnDelay(
            baseStats.maxSpawnDelay() + getMaxSpawnDelayModifier()
        );
        
        int newSpawnCount = SpawnerModuleConfig.clampSpawnCount(
            baseStats.spawnCount() + getSpawnCountModifier()
        );
        
        int newRequiredPlayerRange = shouldIgnorePlayer() ? 
            SpawnerModuleConfig.PLAYER_IGNORER_RANGE : baseStats.requiredPlayerRange();
        
        return new SpawnerStats(
            newSpawnRange,
            newMinDelay,
            newMaxDelay,
            newSpawnCount,
            baseStats.maxNearbyEntities(),
            newRequiredPlayerRange
        );
    }
    
    /**
     * 获取所有已安装模块的信息
     */
    public List<String> getInstalledModulesInfo() {
        List<String> info = new ArrayList<>();
        for (Map.Entry<SpawnerModuleType, Integer> entry : moduleCount.entrySet()) {
            if (entry.getValue() > 0) {
                SpawnerModuleType type = entry.getKey();
                int count = entry.getValue();
                info.add(type.getDisplayName() + " x" + count);
            }
        }
        return info;
    }
    
    /**
     * 通知客户端渲染器更新（仅在客户端调用）
     */
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private void notifyRenderUpdate() {
        // 这个方法会在模块变化时被调用，但实际的渲染更新会在下次渲染时自动获取最新数据
        System.out.println("SpawnerModuleManager: Notified render update");
    }

    /**
     * 刷怪器统计数据记录
     */
    public record SpawnerStats(
        int spawnRange,
        int minSpawnDelay,
        int maxSpawnDelay,
        int spawnCount,
        int maxNearbyEntities,
        int requiredPlayerRange
    ) {}
}
