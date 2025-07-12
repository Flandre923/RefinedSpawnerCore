package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.StructureManager;
import net.minecraft.util.random.WeightedList;
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

import java.util.List;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import com.example.examplemod.spawner.SpawnerModuleManager;
import com.example.examplemod.spawner.SpawnerModuleType;

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
    private SpawnerModuleManager moduleManager = new SpawnerModuleManager(6); // 6个模块槽位

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
        
        // 检查是否有玩家在附近（考虑模块效果）
        int effectivePlayerRange = blockEntity.moduleManager.shouldIgnorePlayer() ?
            1000 : blockEntity.requiredPlayerRange;
        if (!serverLevel.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, effectivePlayerRange)) {
            return;
        }

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

        // 尝试生成生物（使用增强后的数量）
        boolean spawned = false;
        for (int i = 0; i < enhancedStats.spawnCount(); i++) {
            if (blockEntity.spawnMob(serverLevel, pos, enhancedStats.spawnRange())) {
                spawned = true;
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

        // 保存物品数据
        ContainerHelper.saveAllItems(output, this.items);

        // 保存模块数据
        ContainerHelper.saveAllItems(output, this.moduleManager.getModuleSlots(), true);
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

        // 加载物品数据
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);

        // 加载模块数据
        ContainerHelper.loadAllItems(input, this.moduleManager.getModuleSlots());
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
}
