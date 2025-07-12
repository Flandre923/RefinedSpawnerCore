package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.network.FriendlyByteBuf;

public class MobSpawnerMenu extends AbstractContainerMenu {
    private final MobSpawnerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final BlockPos blockPos;

    // 数据索引常量
    public static final int DATA_SPAWN_DELAY = 0;
    public static final int DATA_MIN_SPAWN_DELAY = 1;
    public static final int DATA_MAX_SPAWN_DELAY = 2;
    public static final int DATA_SPAWN_COUNT = 3;
    public static final int DATA_MAX_NEARBY_ENTITIES = 4;
    public static final int DATA_REQUIRED_PLAYER_RANGE = 5;
    public static final int DATA_SPAWN_RANGE = 6;
    public static final int DATA_COUNT = 7;

    // 客户端构造函数
    public MobSpawnerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(ExampleMod.MOB_SPAWNER_MENU.get(), containerId);
        this.blockEntity = null;
        this.level = playerInventory.player.level();

        // 读取服务端发送的数据
        this.blockPos = extraData.readBlockPos();

        // 创建客户端数据容器并读取初始值
        SimpleContainerData clientData = new SimpleContainerData(DATA_COUNT);
        clientData.set(DATA_SPAWN_DELAY, extraData.readInt());
        clientData.set(DATA_MIN_SPAWN_DELAY, extraData.readInt());
        clientData.set(DATA_MAX_SPAWN_DELAY, extraData.readInt());
        clientData.set(DATA_SPAWN_COUNT, extraData.readInt());
        clientData.set(DATA_MAX_NEARBY_ENTITIES, extraData.readInt());
        clientData.set(DATA_REQUIRED_PLAYER_RANGE, extraData.readInt());
        clientData.set(DATA_SPAWN_RANGE, extraData.readInt());

        this.data = clientData;
        addDataSlots(this.data);
    }

    // 服务端构造函数
    public MobSpawnerMenu(int containerId, Inventory playerInventory, MobSpawnerBlockEntity blockEntity, ContainerData data) {
        this(containerId, playerInventory, blockEntity, data, blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO);
    }

    // 通用构造函数
    public MobSpawnerMenu(int containerId, Inventory playerInventory, MobSpawnerBlockEntity blockEntity, ContainerData data, BlockPos blockPos) {
        super(ExampleMod.MOB_SPAWNER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;
        this.blockPos = blockPos != null ? blockPos : (blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO);

        addDataSlots(data);
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity != null) {
            return this.blockEntity.stillValid(player);
        }
        return true;
    }

    // 获取数据的便捷方法
    public int getSpawnDelay() {
        return this.data.get(DATA_SPAWN_DELAY);
    }

    public int getMinSpawnDelay() {
        return this.data.get(DATA_MIN_SPAWN_DELAY);
    }

    public int getMaxSpawnDelay() {
        return this.data.get(DATA_MAX_SPAWN_DELAY);
    }

    public int getSpawnCount() {
        return this.data.get(DATA_SPAWN_COUNT);
    }

    public int getMaxNearbyEntities() {
        return this.data.get(DATA_MAX_NEARBY_ENTITIES);
    }

    public int getRequiredPlayerRange() {
        return this.data.get(DATA_REQUIRED_PLAYER_RANGE);
    }

    public int getSpawnRange() {
        return this.data.get(DATA_SPAWN_RANGE);
    }

    // 设置数据的方法（用于网络同步）
    public void setSpawnDelay(int value) {
        if (this.blockEntity != null) {
            this.blockEntity.setSpawnDelay(value);
        }
    }

    public void setMinSpawnDelay(int value) {
        if (this.blockEntity != null) {
            this.blockEntity.setMinSpawnDelay(value);
        }
    }

    public void setMaxSpawnDelay(int value) {
        if (this.blockEntity != null) {
            this.blockEntity.setMaxSpawnDelay(value);
        }
    }

    public void setSpawnCount(int value) {
        if (this.blockEntity != null) {
            this.blockEntity.setSpawnCount(value);
        }
    }

    public void setMaxNearbyEntities(int value) {
        if (this.blockEntity != null) {
            this.blockEntity.setMaxNearbyEntities(value);
        }
    }

    public void setRequiredPlayerRange(int value) {
        if (this.blockEntity != null) {
            this.blockEntity.setRequiredPlayerRange(value);
        }
    }

    public void setSpawnRange(int value) {
        if (this.blockEntity != null) {
            this.blockEntity.setSpawnRange(value);
        }
    }

    public MobSpawnerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }
}
