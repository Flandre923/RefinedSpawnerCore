package com.example.examplemod.spawner;

/**
 * 刷怪器模块配置类
 * 集中管理所有模块的效果数值，方便调试和平衡性调整
 */
public class SpawnerModuleConfig {
    
    // ==================== 范围相关配置 ====================
    
    /** 范围缩减器：减少的刷怪范围 */
    public static final int RANGE_REDUCER_VALUE = -1;
    
    /** 范围扩展器：增加的刷怪范围 */
    public static final int RANGE_EXPANDER_VALUE = 1;
    
    // ==================== 延迟相关配置 ====================
    
    /** 最小延迟缩减器：减少的最小刷怪延迟（tick） */
    public static final int MIN_DELAY_REDUCER_VALUE = -12;
    
    /** 最大延迟缩减器：减少的最大刷怪延迟（tick） */
    public static final int MAX_DELAY_REDUCER_VALUE = -48;
    
    // ==================== 数量相关配置 ====================
    
    /** 数量增强器：增加的刷怪数量 */
    public static final int COUNT_BOOSTER_VALUE = 1;
    
    /** 最大附近实体增强器：增加的最大附近实体数量 */
    public static final int MAX_NEARBY_ENTITIES_BOOSTER_VALUE = 4;
    
    // ==================== 特殊效果配置 ====================

    /** 玩家忽略器：完全跳过玩家距离检查 */
    public static final boolean PLAYER_IGNORER_ENABLED = true;
    
    // ==================== 限制配置 ====================
    
    /** 刷怪范围的最小值 */
    public static final int MIN_SPAWN_RANGE = 1;
    
    /** 刷怪范围的最大值 */
    public static final int MAX_SPAWN_RANGE = 16;
    
    /** 最小刷怪延迟的最小值 */
    public static final int MIN_SPAWN_DELAY_LIMIT = 1;
    
    /** 最大刷怪延迟的最小值 */
    public static final int MAX_SPAWN_DELAY_LIMIT = 10;
    
    /** 刷怪数量的最小值 */
    public static final int MIN_SPAWN_COUNT = 1;
    
    /** 刷怪数量的最大值 */
    public static final int MAX_SPAWN_COUNT = 16;
    
    /** 最大附近实体数量的最小值 */
    public static final int MIN_MAX_NEARBY_ENTITIES = 1;
    
    /** 最大附近实体数量的最大值 */
    public static final int MAX_MAX_NEARBY_ENTITIES = 64;
    
    // ==================== 堆叠配置 ====================
    
    /** 是否允许同类型模块堆叠效果 */
    public static final boolean ALLOW_STACKING = true;
    
    /** 每种模块类型的最大堆叠数量 */
    public static final int MAX_STACK_PER_TYPE = 3;
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取指定模块类型的效果值
     */
    public static int getModuleEffectValue(SpawnerModuleType type) {
        return switch (type) {
            case RANGE_REDUCER -> RANGE_REDUCER_VALUE;
            case RANGE_EXPANDER -> RANGE_EXPANDER_VALUE;
            case MIN_DELAY_REDUCER -> MIN_DELAY_REDUCER_VALUE;
            case MAX_DELAY_REDUCER -> MAX_DELAY_REDUCER_VALUE;
            case COUNT_BOOSTER -> COUNT_BOOSTER_VALUE;
            case PLAYER_IGNORER -> 0; // 玩家忽略器不需要数值效果
        };
    }
    
    /**
     * 限制数值在合理范围内
     */
    public static int clampSpawnRange(int value) {
        return Math.max(MIN_SPAWN_RANGE, Math.min(MAX_SPAWN_RANGE, value));
    }
    
    public static int clampMinSpawnDelay(int value) {
        return Math.max(MIN_SPAWN_DELAY_LIMIT, value);
    }
    
    public static int clampMaxSpawnDelay(int value) {
        return Math.max(MAX_SPAWN_DELAY_LIMIT, value);
    }
    
    public static int clampSpawnCount(int value) {
        return Math.max(MIN_SPAWN_COUNT, Math.min(MAX_SPAWN_COUNT, value));
    }
    
    public static int clampMaxNearbyEntities(int value) {
        return Math.max(MIN_MAX_NEARBY_ENTITIES, Math.min(MAX_MAX_NEARBY_ENTITIES, value));
    }
}
