package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.redstone.RedstoneMode;
import com.example.examplemod.spawner.SpawnerModuleConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.StructureManager;
import net.minecraft.util.random.WeightedList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import java.lang.reflect.Method;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import com.example.examplemod.spawner.SpawnerModuleManager;
import com.example.examplemod.spawner.SpawnerModuleType;
import com.example.examplemod.util.SpawnerFakePlayer;
import com.example.examplemod.util.ExperienceFluidHelper;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.resources.ResourceKey;
import java.util.ArrayList;

public class MobSpawnerBlockEntity extends BlockEntity implements MenuProvider, Container {
    private static final int SPAWN_RANGE = 4; // 9x9区域的半径
    private static final int SPAWN_DELAY = 100; // 5秒 (20 ticks/秒 * 5)
    private static final int MAX_NEARBY_ENTITIES = 6; // 附近最大实体数量
    private static final int SPAWN_COUNT = 1; // 每次生成的实体数量
    
    private int spawnDelay = SPAWN_DELAY;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = SPAWN_COUNT;
    private int maxNearbyEntities = MAX_NEARBY_ENTITIES;
    private int requiredPlayerRange = 16;
    private int spawnRange = SPAWN_RANGE;

    // 生成位置偏移
    private int offsetX = 0;
    private int offsetY = 0;
    private int offsetZ = 0;

    // 物品槽位 - 用于存储刷怪蛋
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    // 模块管理器 - 管理刷怪器增强模块
    private SpawnerModuleManager moduleManager = new SpawnerModuleManager(9); // 9个模块槽位（7个基础+2个升级）

    // 红石控制模式
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS; // 默认始终工作

    // ContainerData for GUI synchronization
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case MobSpawnerMenu.DATA_SPAWN_DELAY -> MobSpawnerBlockEntity.this.spawnDelay;
                case MobSpawnerMenu.DATA_MIN_SPAWN_DELAY -> MobSpawnerBlockEntity.this.minSpawnDelay;
                case MobSpawnerMenu.DATA_MAX_SPAWN_DELAY -> MobSpawnerBlockEntity.this.maxSpawnDelay;
                case MobSpawnerMenu.DATA_SPAWN_COUNT -> MobSpawnerBlockEntity.this.spawnCount;
                case MobSpawnerMenu.DATA_MAX_NEARBY_ENTITIES -> MobSpawnerBlockEntity.this.maxNearbyEntities;
                case MobSpawnerMenu.DATA_REQUIRED_PLAYER_RANGE -> MobSpawnerBlockEntity.this.requiredPlayerRange;
                case MobSpawnerMenu.DATA_SPAWN_RANGE -> MobSpawnerBlockEntity.this.spawnRange;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case MobSpawnerMenu.DATA_SPAWN_DELAY -> MobSpawnerBlockEntity.this.spawnDelay = value;
                case MobSpawnerMenu.DATA_MIN_SPAWN_DELAY -> MobSpawnerBlockEntity.this.minSpawnDelay = value;
                case MobSpawnerMenu.DATA_MAX_SPAWN_DELAY -> MobSpawnerBlockEntity.this.maxSpawnDelay = value;
                case MobSpawnerMenu.DATA_SPAWN_COUNT -> MobSpawnerBlockEntity.this.spawnCount = value;
                case MobSpawnerMenu.DATA_MAX_NEARBY_ENTITIES -> MobSpawnerBlockEntity.this.maxNearbyEntities = value;
                case MobSpawnerMenu.DATA_REQUIRED_PLAYER_RANGE -> MobSpawnerBlockEntity.this.requiredPlayerRange = value;
                case MobSpawnerMenu.DATA_SPAWN_RANGE -> MobSpawnerBlockEntity.this.spawnRange = value;
            }
        }

        @Override
        public int getCount() {
            return MobSpawnerMenu.DATA_COUNT;
        }
    };

    public MobSpawnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ExampleMod.MOB_SPAWNER_BLOCK_ENTITY.get(), pos, blockState);

        // 设置模块变更监听器
        this.moduleManager.setChangeListener(() -> {
            this.setChanged(); // 标记为已更改，确保数据保存
            System.out.println("MobSpawnerBlockEntity: Module configuration changed");
        });
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MobSpawnerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        
        // 检查红石控制条件
        if (!blockEntity.shouldWorkWithRedstone()) {
            return; // 红石条件不满足，停止工作
        }

        // 检查是否有玩家在附近（考虑模块效果）
        if (!blockEntity.moduleManager.shouldIgnorePlayer()) {
            if (!serverLevel.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, blockEntity.requiredPlayerRange)) {
                return;
            }
        }
        // 如果有玩家忽略模块，直接跳过玩家检查

        // 减少生成延迟
        if (blockEntity.spawnDelay > 0) {
            blockEntity.spawnDelay--;
            return;
        }

        // 获取模块增强后的数值
        SpawnerModuleManager.SpawnerStats baseStats = new SpawnerModuleManager.SpawnerStats(
            blockEntity.spawnRange, blockEntity.minSpawnDelay, blockEntity.maxSpawnDelay,
            blockEntity.spawnCount, blockEntity.maxNearbyEntities, blockEntity.requiredPlayerRange
        );
        SpawnerModuleManager.SpawnerStats enhancedStats = blockEntity.moduleManager.applyModules(baseStats);

        // 调试信息：显示基础值和增强值的对比
        if (blockEntity.spawnDelay <= 0) { // 只在即将生成时打印，避免刷屏
            System.out.println("MobSpawnerBlockEntity: Base stats: " + baseStats);
            System.out.println("MobSpawnerBlockEntity: Enhanced stats: " + enhancedStats);
            System.out.println("MobSpawnerBlockEntity: Active modules: " + blockEntity.moduleManager.getInstalledModulesInfo());
        }

        // 检查附近实体数量（使用增强后的范围）
        AABB checkArea = new AABB(pos).inflate(enhancedStats.spawnRange());
        List<Mob> nearbyMobs = serverLevel.getEntitiesOfClass(Mob.class, checkArea);
        if (nearbyMobs.size() >= enhancedStats.maxNearbyEntities()) {
            return;
        }

        // 检查是否启用模拟升级模式
        boolean spawned = false;
        if (blockEntity.moduleManager.hasSimulationUpgrade()) {
            // 模拟升级模式：直接生成掉落物
            for (int i = 0; i < enhancedStats.spawnCount(); i++) {
                if (blockEntity.simulateSpawn(serverLevel, pos, enhancedStats.spawnRange())) {
                    spawned = true;
                }
            }
        } else {
            // 正常模式：生成生物
            for (int i = 0; i < enhancedStats.spawnCount(); i++) {
                if (blockEntity.spawnMob(serverLevel, pos, enhancedStats.spawnRange())) {
                    spawned = true;
                }
            }
        }

        if (spawned) {
            // 重置生成延迟（使用增强后的延迟）
            int minDelay = Math.max(1, enhancedStats.minSpawnDelay());
            int maxDelay = Math.max(minDelay, enhancedStats.maxSpawnDelay());
            blockEntity.spawnDelay = minDelay + serverLevel.random.nextInt(maxDelay - minDelay + 1);
        }
    }

    private boolean spawnMob(ServerLevel level, BlockPos spawnerPos, int effectiveSpawnRange) {
        EntityType<?> entityType = null;
        RandomSource random = level.random;

        // 检查是否有刷怪蛋
        ItemStack spawnEggStack = this.items.get(0);
        if (!spawnEggStack.isEmpty() && spawnEggStack.getItem() instanceof SpawnEggItem spawnEggItem) {
            // 使用刷怪蛋指定的生物类型
            entityType = spawnEggItem.getType(level.registryAccess(), spawnEggStack);
        } else {
            // 使用原来的逻辑 - 根据生物群系生成
            WeightedList<MobSpawnSettings.SpawnerData> possibleSpawns = getMobsAtPosition(
                level,
                spawnerPos,
                MobCategory.MONSTER
            );

            if (possibleSpawns.isEmpty()) {
                return false;
            }

            // 随机选择一个敌对生物类型
            Optional<MobSpawnSettings.SpawnerData> optionalSpawnerData = possibleSpawns.getRandom(random);
            if (optionalSpawnerData.isEmpty()) {
                return false;
            }

            MobSpawnSettings.SpawnerData spawnerData = optionalSpawnerData.get();
            entityType = spawnerData.type();
        }

        // 在指定区域内随机选择生成位置，考虑偏移
        BlockPos centerPos = spawnerPos.offset(offsetX, offsetY, offsetZ);
        for (int attempts = 0; attempts < 50; attempts++) {
            int x = centerPos.getX() + random.nextInt(2 * effectiveSpawnRange + 1) - effectiveSpawnRange;
            int z = centerPos.getZ() + random.nextInt(2 * effectiveSpawnRange + 1) - effectiveSpawnRange;
            int y = centerPos.getY() + random.nextInt(2 * effectiveSpawnRange + 1) - effectiveSpawnRange;

            BlockPos spawnPos = new BlockPos(x, y, z);

            // 检查生成位置是否合适 - 只需要当前位置是空气即可
            if (!level.getBlockState(spawnPos).isAir()) {
                continue;
            }

            // 检查生物是否有足够的空间（2格高度）
            if (!level.getBlockState(spawnPos.above()).isAir()) {
                continue;
            }
            // 创建实体
            Entity entity = entityType.create(level, EntitySpawnReason.SPAWNER);
            if (entity == null || !(entity instanceof Mob)) {
                continue;
            }
            Mob mob = (Mob) entity;
            mob.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            mob.setYRot(level.random.nextFloat() * 360.0F);
            mob.yHeadRot = mob.getYRot();
            mob.yBodyRot = mob.getYRot();

            // 检查实体是否可以在此位置生成
            if (!mob.checkSpawnRules(level, EntitySpawnReason.SPAWNER) || !mob.checkSpawnObstruction(level)) {
                continue;
            }
            // 生成实体
            level.addFreshEntity(mob);
            // 播放生成效果
            level.levelEvent(2004, spawnPos, 0);
            return true;
        }

        return false;
    }

    /**
     * 模拟生成：使用FakePlayer击杀生物来触发原版掠夺机制
     */
    private boolean simulateSpawn(ServerLevel level, BlockPos spawnerPos, int effectiveSpawnRange) {
        EntityType<?> entityType = null;
        RandomSource random = level.random;

        // 检查是否有刷怪蛋
        ItemStack spawnEggStack = this.items.get(0);
        if (!spawnEggStack.isEmpty() && spawnEggStack.getItem() instanceof SpawnEggItem spawnEggItem) {
            // 使用刷怪蛋指定的生物类型
            entityType = spawnEggItem.getType(level.registryAccess(), spawnEggStack);
        } else {
            // 使用原来的逻辑 - 根据生物群系生成
            WeightedList<MobSpawnSettings.SpawnerData> possibleSpawns = getMobsAtPosition(
                level,
                spawnerPos,
                MobCategory.MONSTER
            );

            if (possibleSpawns.isEmpty()) {
                return false;
            }

            // 随机选择一个敌对生物类型
            Optional<MobSpawnSettings.SpawnerData> optionalSpawnerData = possibleSpawns.getRandom(random);
            if (optionalSpawnerData.isEmpty()) {
                return false;
            }

            MobSpawnSettings.SpawnerData spawnerData = optionalSpawnerData.get();
            entityType = spawnerData.type();
        }

        // 创建临时实体
        Entity tempEntity = entityType.create(level, EntitySpawnReason.SPAWNER);
        if (tempEntity == null || !(tempEntity instanceof LivingEntity)) {
            return false;
        }

        LivingEntity livingEntity = (LivingEntity) tempEntity;

        // 在指定区域内随机选择位置
        BlockPos centerPos = spawnerPos.offset(offsetX, offsetY, offsetZ);
        BlockPos spawnPos = centerPos.offset(
            random.nextInt(2 * effectiveSpawnRange + 1) - effectiveSpawnRange,
            random.nextInt(3) - 1, // Y轴偏移较小
            random.nextInt(2 * effectiveSpawnRange + 1) - effectiveSpawnRange
        );

        // 设置实体位置
        livingEntity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

        // 创建FakePlayer并设置假武器
        SpawnerFakePlayer fakePlayer = new SpawnerFakePlayer(level, spawnerPos);
        int lootingLevel = this.moduleManager.getLootingLevel();
        int beheadingLevel = this.moduleManager.getBeheadingLevel();
        fakePlayer.setupFakeWeapon(lootingLevel, beheadingLevel);

        // 使用FakePlayer击杀生物，触发原版掠夺机制
        List<ItemStack> drops = killEntityWithFakePlayer(livingEntity, fakePlayer, level);

        // 尝试将掉落物插入到周围的容器中
        boolean inserted = insertDropsIntoContainers(level, spawnerPos, drops);

        // 生成经验流体
        int experience = ExperienceFluidHelper.getExperienceFromEntity(livingEntity);
        boolean experienceStored = ExperienceFluidHelper.storeExperienceFluid(level, spawnerPos, experience);

        // 播放生成效果（在刷怪器位置）
        level.levelEvent(2004, spawnerPos, 0);
        System.out.println("MobSpawnerBlockEntity: Simulated spawn of " + entityType.getDescriptionId() +
            " with " + drops.size() + " drops and " + experience + " experience using FakePlayer" +
            (inserted ? " (inserted into containers)" : " (dropped to ground)"));

        // 无论是否成功插入容器，都认为生成成功，这样可以正确重置延迟
        return true;
    }

    /**
     * 使用FakePlayer击杀生物，触发原版掠夺机制
     */
    private List<ItemStack> killEntityWithFakePlayer(LivingEntity entity, SpawnerFakePlayer fakePlayer, ServerLevel level) {
        List<ItemStack> drops = new ArrayList<>();

        try {
            // 创建伤害源，指定FakePlayer为攻击者
            DamageSource damageSource = level.damageSources().playerAttack(fakePlayer);

            // 收集掉落物的临时列表
            List<ItemEntity> dropEntities = new ArrayList<>();

            // 设置实体血量为0来模拟死亡（这样斩首事件才会触发）
            entity.setHealth(0.0F);

            // 创建LivingDropsEvent来捕获掉落物
            LivingDropsEvent dropsEvent = new LivingDropsEvent(entity, damageSource, dropEntities, true);

            // 使用战利品表生成掉落物而不是真正杀死实体
            generateDropsFromLootTable(entity, fakePlayer, level, dropEntities);

            // 触发LivingDropsEvent（这会被斩首升级监听）
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(dropsEvent);

            // 收集所有掉落物
            for (ItemEntity itemEntity : dropsEvent.getDrops()) {
                drops.add(itemEntity.getItem().copy());
            }

            // 移除临时实体（不让它真正死亡掉落物品）
            entity.discard();

            System.out.println("MobSpawnerBlockEntity: FakePlayer killed " + entity.getType().getDescriptionId() +
                " with looting " + fakePlayer.getLootingLevel() + ", generated " + drops.size() + " drops");

        } catch (Exception e) {
            System.err.println("MobSpawnerBlockEntity: Error killing entity with FakePlayer: " + e.getMessage());
            e.printStackTrace();
        }

        return drops;
    }

    /**
     * 使用战利品表生成掉落物
     */
    private void generateDropsFromLootTable(LivingEntity entity, SpawnerFakePlayer fakePlayer, ServerLevel level, List<ItemEntity> dropEntities) {
        try {
            // 获取实体的战利品表
            Optional<ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> lootTableKey = entity.getLootTable();
            if (lootTableKey.isEmpty()) {
                return;
            }

            net.minecraft.world.level.storage.loot.LootTable lootTable = level.getServer().reloadableRegistries()
                .getLootTable(lootTableKey.get());

            // 创建战利品上下文
            net.minecraft.world.level.storage.loot.LootParams.Builder builder =
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, entity)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, entity.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,
                        level.damageSources().playerAttack(fakePlayer))
                    .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, fakePlayer)
                    .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_ATTACKING_ENTITY, fakePlayer);

            // 添加FakePlayer的武器作为工具参数（这样抢夺附魔会生效）
            ItemStack weapon = fakePlayer.getMainHandItem();
            if (!weapon.isEmpty()) {
                builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, weapon);
            }

            net.minecraft.world.level.storage.loot.LootParams lootParams =
                builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);

            // 生成掉落物
            List<ItemStack> lootDrops = lootTable.getRandomItems(lootParams);

            // 将掉落物转换为ItemEntity添加到列表中
            for (ItemStack stack : lootDrops) {
                if (!stack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
                    dropEntities.add(itemEntity);
                }
            }

        } catch (Exception e) {
            System.err.println("MobSpawnerBlockEntity: Error generating drops from loot table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取实体的掉落物 - 使用原版战利品表系统（备用方法）
     */
    private List<ItemStack> getEntityDrops(LivingEntity entity, ServerLevel level, BlockPos pos) {
        List<ItemStack> drops = new java.util.ArrayList<>();

        // 设置实体位置用于掉落物计算
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        try {
            // 使用原版的战利品表系统
            java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> lootTableKey = entity.getLootTable();

            if (lootTableKey.isPresent()) {
                // 获取战利品表
                net.minecraft.world.level.storage.loot.LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey.get());

                // 创建战利品上下文
                net.minecraft.world.level.storage.loot.LootParams.Builder builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, entity)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, entity.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().generic());

                // 如果有玩家在附近，添加玩家参数以支持抢夺附魔等
                net.minecraft.world.entity.player.Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16.0, false);
                if (nearestPlayer != null) {
                    builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, nearestPlayer);
                    builder.withLuck(nearestPlayer.getLuck());
                }

                // 应用模拟升级的抢夺和斩首效果
                int lootingLevel = this.moduleManager.getLootingLevel();
                int beheadingLevel = this.moduleManager.getBeheadingLevel();

                System.out.println("MobSpawnerBlockEntity: Looting level: " + lootingLevel + ", Beheading level: " + beheadingLevel);
                System.out.println("MobSpawnerBlockEntity: Has simulation upgrade: " + this.moduleManager.hasSimulationUpgrade());
                System.out.println("MobSpawnerBlockEntity: Module counts: " + this.moduleManager.getInstalledModulesInfo());

                // 创建虚拟武器来应用抢夺效果
                if (lootingLevel > 0) {
                    net.minecraft.world.item.ItemStack virtualWeapon = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
                    // 添加抢夺附魔
                    virtualWeapon.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), lootingLevel);
                    builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, virtualWeapon);
                    System.out.println("MobSpawnerBlockEntity: Applied looting " + lootingLevel + " to virtual weapon");
                }

                net.minecraft.world.level.storage.loot.LootParams lootParams = builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);

                // 获取掉落物
                drops.addAll(lootTable.getRandomItems(lootParams));

                // 应用斩首效果：直接添加头颅掉落
                if (beheadingLevel > 0) {
                    // 计算头颅掉落概率：每级斩首增加10%概率
                    int dropChance = level.random.nextInt(10);
                    if (dropChance < beheadingLevel) {
                        ItemStack headStack = getHeadFromEntity(entity);
                        if (!headStack.isEmpty()) {
                            drops.add(headStack);
                            System.out.println("MobSpawnerBlockEntity: Added head drop for " + entity.getType().getDescriptionId() + " with beheading level " + beheadingLevel);
                        }
                    }
                }

                System.out.println("MobSpawnerBlockEntity: Generated " + drops.size() + " drops using loot table for " + entity.getType().getDescriptionId());
            }
        } catch (Exception e) {
            // 如果战利品表方法失败，使用简化的后备方案
            System.out.println("MobSpawnerBlockEntity: Failed to get loot table drops, using fallback: " + e.getMessage());
        }

        // 如果没有获取到掉落物，使用后备方案
        if (drops.isEmpty()) {
            drops.addAll(getFallbackDrops(entity, level));

            // 在后备方案中也应用斩首效果
            int beheadingLevel = this.moduleManager.getBeheadingLevel();
            if (beheadingLevel > 0) {
                // 计算头颅掉落概率：每级斩首增加10%概率
                int dropChance = level.random.nextInt(10);
                if (dropChance < beheadingLevel) {
                    ItemStack headStack = getHeadFromEntity(entity);
                    if (!headStack.isEmpty()) {
                        drops.add(headStack);
                        System.out.println("MobSpawnerBlockEntity: Added head drop (fallback) for " + entity.getType().getDescriptionId() + " with beheading level " + beheadingLevel);
                    }
                }
            }
        }

        return drops;
    }

    /**
     * 后备掉落物方案
     */
    private List<ItemStack> getFallbackDrops(LivingEntity entity, ServerLevel level) {
        List<ItemStack> drops = new java.util.ArrayList<>();

        // 获取升级等级
        int lootingLevel = this.moduleManager.getLootingLevel();
        int beheadingLevel = this.moduleManager.getBeheadingLevel();

        // 简化的掉落物生成逻辑作为后备（斩首效果由EntityHeadDropEvent处理）
        if (entity instanceof net.minecraft.world.entity.monster.Zombie) {
            int baseAmount = 1 + level.random.nextInt(3);
            int lootingBonus = lootingLevel > 0 ? level.random.nextInt(lootingLevel + 1) : 0;
            drops.add(new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, baseAmount + lootingBonus));
        } else if (entity instanceof net.minecraft.world.entity.monster.Skeleton) {
            int baseAmount = 1 + level.random.nextInt(3);
            int lootingBonus = lootingLevel > 0 ? level.random.nextInt(lootingLevel + 1) : 0;
            drops.add(new ItemStack(net.minecraft.world.item.Items.BONE, baseAmount + lootingBonus));
            drops.add(new ItemStack(net.minecraft.world.item.Items.ARROW, level.random.nextInt(3) + lootingBonus));
        } else if (entity instanceof net.minecraft.world.entity.monster.Creeper) {
            int baseAmount = 1 + level.random.nextInt(3);
            int lootingBonus = lootingLevel > 0 ? level.random.nextInt(lootingLevel + 1) : 0;
            drops.add(new ItemStack(net.minecraft.world.item.Items.GUNPOWDER, baseAmount + lootingBonus));
        } else if (entity instanceof net.minecraft.world.entity.monster.Spider) {
            int baseAmount = 1 + level.random.nextInt(3);
            int lootingBonus = lootingLevel > 0 ? level.random.nextInt(lootingLevel + 1) : 0;
            drops.add(new ItemStack(net.minecraft.world.item.Items.STRING, baseAmount + lootingBonus));
        } else {
            // 默认掉落物
            int baseAmount = 1;
            int lootingBonus = lootingLevel > 0 ? level.random.nextInt(lootingLevel + 1) : 0;
            drops.add(new ItemStack(net.minecraft.world.item.Items.BONE, baseAmount + lootingBonus));
        }

        return drops;
    }

    /**
     * 根据实体类型获取对应的头颅（仅原版头颅）
     */
    private ItemStack getHeadFromEntity(LivingEntity entity) {
        // 只支持原版头颅
        if (entity instanceof net.minecraft.world.entity.monster.Zombie && !(entity instanceof net.minecraft.world.entity.monster.ZombieVillager)) {
            return new ItemStack(net.minecraft.world.item.Items.ZOMBIE_HEAD);
        } else if (entity instanceof net.minecraft.world.entity.monster.Skeleton && !(entity instanceof net.minecraft.world.entity.monster.WitherSkeleton)) {
            return new ItemStack(net.minecraft.world.item.Items.SKELETON_SKULL);
        } else if (entity instanceof net.minecraft.world.entity.monster.Creeper) {
            return new ItemStack(net.minecraft.world.item.Items.CREEPER_HEAD);
        } else if (entity instanceof net.minecraft.world.entity.monster.WitherSkeleton) {
            return new ItemStack(net.minecraft.world.item.Items.WITHER_SKELETON_SKULL);
        } else if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            return new ItemStack(net.minecraft.world.item.Items.DRAGON_HEAD);
        } else if (entity instanceof net.minecraft.world.entity.player.Player player) {
            // 玩家头颅
            ItemStack playerHead = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
            // 设置玩家头颅的皮肤
            playerHead.set(net.minecraft.core.component.DataComponents.PROFILE,
                new net.minecraft.world.item.component.ResolvableProfile(player.getGameProfile()));
            return playerHead;
        }

        // 其他实体不支持头颅掉落
        return ItemStack.EMPTY;
    }



    /**
     * 将掉落物插入到周围的容器中 - 使用NeoForge ItemHandler
     */
    private boolean insertDropsIntoContainers(ServerLevel level, BlockPos spawnerPos, List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return false;
        }

        // 搜索周围的ItemHandler
        List<IItemHandler> itemHandlers = findNearbyItemHandlers(level, spawnerPos);

        if (itemHandlers.isEmpty()) {
            System.out.println("MobSpawnerBlockEntity: No item handlers found nearby");
            return false;
        }

        // 尝试插入所有掉落物
        List<ItemStack> remainingDrops = new ArrayList<>(drops);

        for (IItemHandler handler : itemHandlers) {
            if (remainingDrops.isEmpty()) {
                break;
            }

            // 尝试插入到当前ItemHandler
            remainingDrops = insertItemsIntoItemHandler(handler, remainingDrops);
        }

        // 如果还有剩余物品，掉落到地面
        if (!remainingDrops.isEmpty()) {
            BlockPos dropPos = spawnerPos.above();
            for (ItemStack stack : remainingDrops) {
                ItemEntity itemEntity = new ItemEntity(
                    level, dropPos.getX() + 0.5, dropPos.getY(), dropPos.getZ() + 0.5, stack
                );
                level.addFreshEntity(itemEntity);
            }
            System.out.println("MobSpawnerBlockEntity: Dropped " + remainingDrops.size() + " items to ground");
        }

        boolean allInserted = remainingDrops.isEmpty();
        System.out.println("MobSpawnerBlockEntity: Inserted " + (drops.size() - remainingDrops.size()) + "/" + drops.size() + " items into containers");
        return allInserted;
    }

    /**
     * 查找附近的ItemHandler
     */
    private List<IItemHandler> findNearbyItemHandlers(ServerLevel level, BlockPos spawnerPos) {
        List<IItemHandler> itemHandlers = new ArrayList<>();
        int searchRange = SpawnerModuleConfig.SIMULATION_CONTAINER_SEARCH_RANGE;

        // 搜索周围的方块实体
        for (int x = -searchRange; x <= searchRange; x++) {
            for (int y = -searchRange; y <= searchRange; y++) {
                for (int z = -searchRange; z <= searchRange; z++) {
                    BlockPos checkPos = spawnerPos.offset(x, y, z);
                    net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(checkPos);

                    // 排除自己
                    if (blockEntity == this) {
                        continue;
                    }

                    // 检查是否有ItemHandler capability
                    IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, checkPos, null);
                    if (itemHandler != null) {
                        itemHandlers.add(itemHandler);
                    }
                }
            }
        }

        System.out.println("MobSpawnerBlockEntity: Found " + itemHandlers.size() + " item handlers nearby");
        return itemHandlers;
    }

    /**
     * 将物品插入到ItemHandler中
     */
    private List<ItemStack> insertItemsIntoItemHandler(IItemHandler handler, List<ItemStack> items) {
        List<ItemStack> remainingItems = new ArrayList<>();

        for (ItemStack stack : items) {
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
            if (!remaining.isEmpty()) {
                remainingItems.add(remaining);
            }
        }

        return remainingItems;
    }

    /**
     * 将单个物品插入到容器中
     */
    private ItemStack insertItemIntoContainer(net.minecraft.world.Container container, ItemStack stack) {
        int containerSize = container.getContainerSize();

        // 首先尝试合并到现有堆叠
        for (int i = 0; i < containerSize; i++) {
            ItemStack slotStack = container.getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int maxStackSize = Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
                int canAdd = maxStackSize - slotStack.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, stack.getCount());
                    slotStack.grow(toAdd);
                    stack.shrink(toAdd);
                    container.setChanged();

                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // 然后尝试放入空槽位
        for (int i = 0; i < containerSize; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack.copy());
                container.setChanged();
                return ItemStack.EMPTY;
            }
        }

        // 容器已满，返回剩余物品
        return stack;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SpawnDelay", this.spawnDelay);
        output.putInt("MinSpawnDelay", this.minSpawnDelay);
        output.putInt("MaxSpawnDelay", this.maxSpawnDelay);
        output.putInt("SpawnCount", this.spawnCount);
        output.putInt("MaxNearbyEntities", this.maxNearbyEntities);
        output.putInt("RequiredPlayerRange", this.requiredPlayerRange);
        output.putInt("SpawnRange", this.spawnRange);

        // 保存偏移数据
        output.putInt("OffsetX", this.offsetX);
        output.putInt("OffsetY", this.offsetY);
        output.putInt("OffsetZ", this.offsetZ);

        // 保存红石控制模式
        output.putString("RedstoneMode", this.redstoneMode.getSerializedName());

        // 保存物品数据
        ContainerHelper.saveAllItems(output, this.items);

        // 保存模块数据 - 使用不同的key避免冲突
        saveModuleItems(output, this.moduleManager.getModuleSlots());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 加载数据，如果不存在则使用默认值
        this.spawnDelay = input.getInt("SpawnDelay").orElse(this.spawnDelay);
        this.minSpawnDelay = input.getInt("MinSpawnDelay") .orElse(this.minSpawnDelay);
        this.maxSpawnDelay = input.getInt("MaxSpawnDelay") .orElse(this.maxSpawnDelay);
        this.spawnCount = input.getInt("SpawnCount").orElse(this.spawnCount);
        this.maxNearbyEntities = input.getInt("MaxNearbyEntities").orElse(this.maxNearbyEntities);
        this.requiredPlayerRange = input.getInt("RequiredPlayerRange").orElse(this.requiredPlayerRange);
        this.spawnRange = input.getInt("SpawnRange").orElse(this.spawnRange) ;

        // 加载偏移数据
        this.offsetX = input.getInt("OffsetX").orElse(0);
        this.offsetY = input.getInt("OffsetY").orElse(0);
        this.offsetZ = input.getInt("OffsetZ").orElse(0);

        // 加载红石控制模式
        String redstoneModeStr = input.getString("RedstoneMode").orElse("always");
        this.redstoneMode = RedstoneMode.fromString(redstoneModeStr);

        // 加载物品数据
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);

        // 加载模块数据 - 使用不同的key避免冲突
        loadModuleItems(input, this.moduleManager.getModuleSlots());
        this.moduleManager.recalculateModules();
    }

    // MenuProvider implementation
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.examplemod.mob_spawner");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 确保数据同步到客户端
        this.syncDataToClient();
        return new MobSpawnerMenu(containerId, playerInventory, this, this.dataAccess);
    }

    /**
     * 同步数据到客户端
     */
    private void syncDataToClient() {
        // 触发数据更新，确保ContainerData中的值是最新的
        for (int i = 0; i < MobSpawnerMenu.DATA_COUNT; i++) {
            this.dataAccess.set(i, this.dataAccess.get(i));
        }
    }

    // Setter methods for GUI
    public void setSpawnDelay(int spawnDelay) {
        this.spawnDelay = spawnDelay;
        this.setChanged();
    }

    public void setMinSpawnDelay(int minSpawnDelay) {
        this.minSpawnDelay = minSpawnDelay;
        this.setChanged();
    }

    public void setMaxSpawnDelay(int maxSpawnDelay) {
        this.maxSpawnDelay = maxSpawnDelay;
        this.setChanged();
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
        this.setChanged();
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
        this.setChanged();
    }

    public void setRequiredPlayerRange(int requiredPlayerRange) {
        this.requiredPlayerRange = requiredPlayerRange;
        this.setChanged();
    }

    public void setSpawnRange(int spawnRange) {
        this.spawnRange = spawnRange;
        this.setChanged();
    }

    // Getter methods for GUI
    public int getSpawnDelay() { return this.spawnDelay; }
    public int getMinSpawnDelay() { return this.minSpawnDelay; }
    public int getMaxSpawnDelay() { return this.maxSpawnDelay; }
    public int getSpawnCount() { return this.spawnCount; }
    public int getMaxNearbyEntities() { return this.maxNearbyEntities; }
    public int getRequiredPlayerRange() { return this.requiredPlayerRange; }
    public int getSpawnRange() { return this.spawnRange; }

    // 偏移位置的getter和setter
    public int getOffsetX() { return this.offsetX; }
    public int getOffsetY() { return this.offsetY; }
    public int getOffsetZ() { return this.offsetZ; }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
        this.setChanged();
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
        this.setChanged();
    }

    public void setOffsetZ(int offsetZ) {
        this.offsetZ = offsetZ;
        this.setChanged();
    }

    // 模块管理器相关方法
    public SpawnerModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * 获取当前红石控制模式
     */
    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    /**
     * 设置红石控制模式
     */
    public void setRedstoneMode(RedstoneMode mode) {
        this.redstoneMode = mode;
        setChanged();

        // 同步到客户端
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            // 发送专门的客户端同步包给附近的玩家
            var syncPacket = new com.example.examplemod.network.RedstoneModeClientSyncPacket(worldPosition, mode);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(
                (net.minecraft.server.level.ServerLevel) level,
                null, // 不排除任何玩家
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                64.0, // 64格范围内的玩家
                syncPacket
            );
        }

        System.out.println("MobSpawnerBlockEntity: Set redstone mode to " + mode.getDisplayName() + " at " + worldPosition);
    }

    /**
     * 客户端专用的红石模式设置方法（不触发保存和同步）
     */
    public void setRedstoneModeClient(RedstoneMode mode) {
        this.redstoneMode = mode;
        System.out.println("MobSpawnerBlockEntity: Client-side redstone mode updated to " + mode.getDisplayName() + " at " + worldPosition);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("RedstoneMode", this.redstoneMode.getSerializedName());
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        input.getString("RedstoneMode").ifPresent((mode)->{
            this.redstoneMode = RedstoneMode.fromString(mode);
            System.out.println("MobSpawnerBlockEntity: Client received redstone mode update: " + this.redstoneMode.getDisplayName());
        });
    }

    /**
     * 获取当前位置的红石信号强度
     */
    private int getRedstonePower() {
        if (level == null) {
            return 0;
        }
        return level.getBestNeighborSignal(worldPosition);
    }

    /**
     * 检查是否应该根据红石模式工作
     */
    private boolean shouldWorkWithRedstone() {
        int redstonePower = getRedstonePower();
        return redstoneMode.shouldWork(redstonePower);
    }


    /**
     * 使用与NaturalSpawner相同的逻辑获取指定位置的可生成生物
     * 这会自动处理结构生物生成规则
     */
    private WeightedList<MobSpawnSettings.SpawnerData> getMobsAtPosition(ServerLevel level, BlockPos pos, MobCategory category) {
        try {
            // 获取必要的组件
            StructureManager structureManager = level.structureManager();
            var chunkGenerator = level.getChunkSource().getGenerator();
            var biome = level.getBiome(pos);

            // 使用与NaturalSpawner.mobsAt相同的逻辑
            // 首先检查是否在下界要塞中
            if (isInNetherFortressBounds(pos, level, category, structureManager)) {
                var registryAccess = structureManager.registryAccess();
                var structureRegistry = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                var fortress = structureRegistry.getValueOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.FORTRESS);
                var monsterSpawns = fortress.spawnOverrides().get(MobCategory.MONSTER);
                if (monsterSpawns != null) {
                    return EventHooks.getPotentialSpawns(level, category, pos, monsterSpawns.spawns());
                }
            }

            // 使用标准的生物群系+结构生成逻辑
            var biomeMobs = chunkGenerator.getMobsAt(biome, structureManager, category, pos);
            return EventHooks.getPotentialSpawns(level, category, pos, biomeMobs);

        } catch (Exception e) {
            // 如果出现任何错误，回退到基本的生物群系生成
            Biome biome = level.getBiome(pos).value();
            WeightedList<MobSpawnSettings.SpawnerData> biomeMobs = biome.getMobSettings().getMobs(category);
            return EventHooks.getPotentialSpawns(level, category, pos, biomeMobs);
        }
    }

    /**
     * 检查位置是否在下界要塞边界内
     * 复制自NaturalSpawner.isInNetherFortressBounds
     */
    private boolean isInNetherFortressBounds(BlockPos pos, ServerLevel level, MobCategory category, StructureManager structureManager) {
        if (category == MobCategory.MONSTER && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.NETHER_BRICKS)) {
            var registryAccess = structureManager.registryAccess();
            var structureRegistry = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            var structure = structureRegistry.getValue(net.minecraft.world.level.levelgen.structure.BuiltinStructures.FORTRESS);
            return structure != null && structureManager.getStructureAt(pos, structure).isValid();
        }
        return false;
    }

    // Container接口实现
    @Override
    public int getContainerSize() {
        return this.items.size(); // 只返回刷怪蛋槽位，模块槽位通过单独的接口访问
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack itemstack = this.items.get(index);
        if (!itemstack.isEmpty()) {
            if (itemstack.getCount() <= count) {
                this.items.set(index, ItemStack.EMPTY);
                this.setChanged();
                return itemstack;
            } else {
                ItemStack result = itemstack.split(count);
                this.setChanged();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack itemstack = this.items.get(index);
        this.items.set(index, ItemStack.EMPTY);
        return itemstack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.items.set(index, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this &&
               player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        // 只允许放置刷怪蛋
        return stack.getItem() instanceof SpawnEggItem;
    }

    @Override
    public void startOpen(Player player) {
        // 当玩家打开容器时调用
    }

    @Override
    public void stopOpen(Player player) {
        // 当玩家关闭容器时调用
    }

    // 打开刷怪蛋界面的方法
    public void openSpawnEggMenu(ServerPlayer player) {
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.examplemod.spawn_egg_mob_spawner");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
                return new com.example.examplemod.blockentity.SpawnEggMobSpawnerMenu(containerId, playerInventory, MobSpawnerBlockEntity.this);
            }
        }, (buf) -> {
            buf.writeBlockPos(this.getBlockPos());
            // 发送刷怪范围数据，确保客户端能获取到正确的值
            buf.writeInt(this.getSpawnRange());
            // 发送偏移数据
            buf.writeInt(this.getOffsetX());
            buf.writeInt(this.getOffsetY());
            buf.writeInt(this.getOffsetZ());
        });
    }

    /**
     * 保存模块物品数据，使用自定义key避免与主物品数据冲突
     */
    private void saveModuleItems(ValueOutput output, NonNullList<ItemStack> moduleItems) {
        var typedOutputList = output.list("ModuleItems", ItemStackWithSlot.CODEC);

        for (int i = 0; i < moduleItems.size(); ++i) {
            ItemStack itemStack = moduleItems.get(i);
            if (!itemStack.isEmpty()) {
                typedOutputList.add(new ItemStackWithSlot(i, itemStack));
            }
        }
    }

    /**
     * 加载模块物品数据，使用自定义key避免与主物品数据冲突
     */
    private void loadModuleItems(ValueInput input, NonNullList<ItemStack> moduleItems) {
        var moduleItemsList = input.listOrEmpty("ModuleItems", ItemStackWithSlot.CODEC);

        for (var itemStackWithSlot : moduleItemsList) {
            if (itemStackWithSlot.isValidInContainer(moduleItems.size())) {
                moduleItems.set(itemStackWithSlot.slot(), itemStackWithSlot.stack());
            }
        }
    }
}
