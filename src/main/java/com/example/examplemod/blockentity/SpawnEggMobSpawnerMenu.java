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
    private final int offsetX, offsetY, offsetZ; // 存储从服务端接收的偏移数据

    // 客户端构造函数
    public SpawnEggMobSpawnerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(1), extraData.readBlockPos(), extraData.readInt(),
             extraData.readInt(), extraData.readInt(), extraData.readInt());
    }

    // 服务端构造函数
    public SpawnEggMobSpawnerMenu(int containerId, Inventory playerInventory, MobSpawnerBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.getBlockPos(), blockEntity.getSpawnRange(),
             blockEntity.getOffsetX(), blockEntity.getOffsetY(), blockEntity.getOffsetZ());
    }

    // 通用构造函数
    public SpawnEggMobSpawnerMenu(int containerId, Inventory playerInventory, Container container, BlockPos blockPos,
                                 int spawnRange, int offsetX, int offsetY, int offsetZ) {
        super(ExampleMod.SPAWN_EGG_MOB_SPAWNER_MENU.get(), containerId);
        checkContainerSize(container, 1);
        this.container = container;
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.spawnRange = spawnRange;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;

        container.startOpen(playerInventory.player);

        // 添加刷怪蛋槽位
        this.addSlot(new SpawnEggSlot(container, 0, 80, 35));

        // 添加模块槽位 (6个槽位，3x2布局)
        addModuleSlots();

        // 添加玩家物品栏
        this.addPlayerInventory(playerInventory);
    }

    private void addModuleSlots() {
        // 获取模块管理器
        if (this.level != null && this.level.isLoaded(this.blockPos)) {
            var blockEntity = this.level.getBlockEntity(this.blockPos);
            if (blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity spawner) {
                var moduleManager = spawner.getModuleManager();

                // 添加6个模块槽位，3x2布局
                for (int i = 0; i < 6; i++) {
                    int x = 8 + (i % 3) * 18;  // 3列
                    int y = 60 + (i / 3) * 18; // 2行
                    this.addSlot(new ModuleSlot(moduleManager, i, x, y));
                }
            }
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // 玩家背包 (向下移动以给模块槽位让出空间)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 120 + i * 18));
            }
        }

        // 玩家快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 178));
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

    public int getOffsetX() {
        return this.offsetX;
    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public int getOffsetZ() {
        return this.offsetZ;
    }

    public Level getLevel() {
        return this.level;
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

    // 模块槽位类
    private static class ModuleSlot extends Slot {
        private final com.example.examplemod.spawner.SpawnerModuleManager moduleManager;
        private final int moduleIndex;

        public ModuleSlot(com.example.examplemod.spawner.SpawnerModuleManager moduleManager, int moduleIndex, int x, int y) {
            super(new ModuleContainer(moduleManager), moduleIndex, x, y);
            this.moduleManager = moduleManager;
            this.moduleIndex = moduleIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return com.example.examplemod.spawner.SpawnerModuleType.isValidModule(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 16;
        }
    }

    // 模块容器适配器
    private static class ModuleContainer implements Container {
        private final com.example.examplemod.spawner.SpawnerModuleManager moduleManager;

        public ModuleContainer(com.example.examplemod.spawner.SpawnerModuleManager moduleManager) {
            this.moduleManager = moduleManager;
        }

        @Override
        public int getContainerSize() {
            return moduleManager.getModuleSlots().size();
        }

        @Override
        public boolean isEmpty() {
            return moduleManager.getModuleSlots().stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int index) {
            return moduleManager.getModule(index);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack stack = moduleManager.getModule(index);
            if (!stack.isEmpty()) {
                if (stack.getCount() <= count) {
                    moduleManager.setModule(index, ItemStack.EMPTY);
                    moduleManager.recalculateModules(); // 重新计算模块效果
                    System.out.println("ModuleContainer: Removed module at index " + index);
                    return stack;
                } else {
                    ItemStack result = stack.split(count);
                    moduleManager.recalculateModules(); // 重新计算模块效果
                    return result;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack stack = moduleManager.getModule(index);
            moduleManager.setModule(index, ItemStack.EMPTY);
            moduleManager.recalculateModules(); // 重新计算模块效果
            return stack;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            moduleManager.setModule(index, stack);
            moduleManager.recalculateModules(); // 重新计算模块效果
            System.out.println("ModuleContainer: Set module at index " + index + " to " + stack);
        }

        @Override
        public void setChanged() {
            moduleManager.recalculateModules(); // 确保模块效果更新
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < getContainerSize(); i++) {
                moduleManager.setModule(i, ItemStack.EMPTY);
            }
        }
    }
}
