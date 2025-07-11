package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

public class SpawnEggMobSpawnerMenu extends AbstractContainerMenu {
    private final Container container;
    private final Level level;
    private final BlockPos blockPos;
    private final int spawnRange; // 存储从服务端接收的刷怪范围

    // 客户端构造函数
    public SpawnEggMobSpawnerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(1), extraData.readBlockPos(), extraData.readInt());
    }

    // 服务端构造函数
    public SpawnEggMobSpawnerMenu(int containerId, Inventory playerInventory, MobSpawnerBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.getBlockPos(), blockEntity.getSpawnRange());
    }

    // 通用构造函数
    public SpawnEggMobSpawnerMenu(int containerId, Inventory playerInventory, Container container, BlockPos blockPos, int spawnRange) {
        super(ExampleMod.SPAWN_EGG_MOB_SPAWNER_MENU.get(), containerId);
        checkContainerSize(container, 1);
        this.container = container;
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.spawnRange = spawnRange;

        container.startOpen(playerInventory.player);

        // 添加刷怪蛋槽位
        this.addSlot(new SpawnEggSlot(container, 0, 80, 35));

        // 添加玩家物品栏
        this.addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // 玩家背包
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 玩家快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            if (index == 0) {
                // 从刷怪蛋槽位移动到玩家背包
                if (!this.moveItemStackTo(itemstack1, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < this.slots.size()) {
                // 从玩家背包移动到刷怪蛋槽位
                if (itemstack1.getItem() instanceof SpawnEggItem) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
            
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, itemstack1);
        }
        
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public int getSpawnRange() {
        // 优先使用服务端发送的范围值
        if (this.spawnRange > 0) {
            return this.spawnRange;
        }

        if (this.container instanceof MobSpawnerBlockEntity spawner) {
            return spawner.getSpawnRange();
        }

        // 客户端情况：直接从世界中获取BlockEntity
        if (this.level != null && this.level.isLoaded(this.blockPos)) {
            var blockEntity = this.level.getBlockEntity(this.blockPos);
            if (blockEntity instanceof MobSpawnerBlockEntity spawner) {
                return spawner.getSpawnRange();
            }
        }

        return 4; // 默认范围
    }

    // 自定义槽位类，只允许放置刷怪蛋
    private static class SpawnEggSlot extends Slot {
        public SpawnEggSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof SpawnEggItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1; // 只允许放置一个刷怪蛋
        }
    }
}
