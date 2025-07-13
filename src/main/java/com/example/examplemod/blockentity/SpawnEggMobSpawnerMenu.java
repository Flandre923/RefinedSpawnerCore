package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.spawner.SpawnerModuleType;
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

                // 定义每个槽位允许的模块类型（每种类型一个槽位）
                com.example.examplemod.spawner.SpawnerModuleType[] slotTypes = {
                    com.example.examplemod.spawner.SpawnerModuleType.RANGE_REDUCER,    // 槽位0：范围缩减器
                    com.example.examplemod.spawner.SpawnerModuleType.RANGE_EXPANDER,   // 槽位1：范围扩展器
                    com.example.examplemod.spawner.SpawnerModuleType.MIN_DELAY_REDUCER, // 槽位2：最小延迟缩减器
                    com.example.examplemod.spawner.SpawnerModuleType.MAX_DELAY_REDUCER, // 槽位3：最大延迟缩减器
                    com.example.examplemod.spawner.SpawnerModuleType.COUNT_BOOSTER,    // 槽位4：数量增强器
                    com.example.examplemod.spawner.SpawnerModuleType.PLAYER_IGNORER,   // 槽位5：玩家忽略器
                    com.example.examplemod.spawner.SpawnerModuleType.SIMULATION_UPGRADE, // 槽位6：模拟升级
                    null,  // 槽位7：通用槽位，允许任何模块类型
                    com.example.examplemod.spawner.SpawnerModuleType.LOOTING_UPGRADE,  // 槽位8：抢夺升级（仅模拟升级时可见）
                    com.example.examplemod.spawner.SpawnerModuleType.BEHEADING_UPGRADE // 槽位9：斩首升级（仅模拟升级时可见）
                };

                // 添加所有10个模块槽位，重新布局避免与调整按钮重合
                for (int i = 0; i < 10; i++) {
                    int x, y;
                    if (i < 8) {
                        // 前8个槽位：4x2布局，向上移动
                        x = 8 + (i % 4) * 18;
                        y = 45 + (i / 4) * 18; // 向上移动10像素
                    } else {
                        // 额外的2个槽位：放在右侧
                        x = 8 + 4 * 18 + 10 + ((i - 8) % 2) * 18; // 右侧，间隔10像素
                        y = 45 + ((i - 8) / 2) * 18;
                    }
                    this.addSlot(new ModuleSlot(moduleManager, i, x, y, slotTypes[i]));
                }
            }
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // 玩家背包 (向下移动以给模块槽位和按钮让出空间)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 130 + i * 18));
            }
        }

        // 玩家快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 188));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 计算槽位范围
            int spawnEggSlotEnd = 1; // 刷怪蛋槽位：0
            int moduleSlotEnd = spawnEggSlotEnd + 10; // 模块槽位：1-10
            int playerInventoryEnd = moduleSlotEnd + 27; // 玩家背包：11-37
            int hotbarEnd = playerInventoryEnd + 9; // 快捷栏：38-46

            if (index == 0) {
                // 从刷怪蛋槽位移动到玩家背包
                if (!this.moveItemStackTo(itemstack1, moduleSlotEnd, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= spawnEggSlotEnd && index < moduleSlotEnd) {
                // 从模块槽位移动到玩家背包
                if (!this.moveItemStackTo(itemstack1, moduleSlotEnd, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= moduleSlotEnd && index < hotbarEnd) {
                // 从玩家背包移动到相应槽位
                if (itemstack1.getItem() instanceof SpawnEggItem) {
                    if (!this.moveItemStackTo(itemstack1, 0, spawnEggSlotEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getItem() instanceof com.example.examplemod.item.SpawnerModuleItem) {
                    if (!this.moveItemStackTo(itemstack1, spawnEggSlotEnd, moduleSlotEnd, false)) {
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
        private final com.example.examplemod.spawner.SpawnerModuleType allowedType;

        public ModuleSlot(com.example.examplemod.spawner.SpawnerModuleManager moduleManager, int moduleIndex, int x, int y, com.example.examplemod.spawner.SpawnerModuleType allowedType) {
            super(new ModuleContainer(moduleManager), moduleIndex, x, y);
            this.moduleManager = moduleManager;
            this.moduleIndex = moduleIndex;
            this.allowedType = allowedType;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 检查槽位是否可用
            if (!moduleManager.isSlotAvailable(moduleIndex)) {
                return false;
            }

            if (!com.example.examplemod.spawner.SpawnerModuleType.isValidModule(stack)) {
                return false;
            }

            // 如果指定了允许的类型，检查是否匹配
            if (allowedType != null) {
                com.example.examplemod.spawner.SpawnerModuleType moduleType =
                    com.example.examplemod.spawner.SpawnerModuleType.fromItemStack(stack);
                return moduleType == allowedType;
            }

            // 通用槽位允许任何模块类型
            return true;
        }

        @Override
        public boolean isActive() {
            // 只有可用的槽位才是活跃的
            return moduleManager.isSlotAvailable(moduleIndex);
        }

        @Override
        public int getMaxStackSize() {
            // 升级模块和延迟模块可以堆叠到16个，其他模块不能堆叠
            if (allowedType == com.example.examplemod.spawner.SpawnerModuleType.LOOTING_UPGRADE ||
                allowedType == com.example.examplemod.spawner.SpawnerModuleType.BEHEADING_UPGRADE ||
                allowedType == com.example.examplemod.spawner.SpawnerModuleType.MIN_DELAY_REDUCER ||
                allowedType == com.example.examplemod.spawner.SpawnerModuleType.MAX_DELAY_REDUCER ||
                allowedType == SpawnerModuleType.RANGE_REDUCER||
                allowedType == SpawnerModuleType.RANGE_EXPANDER ||
                allowedType == SpawnerModuleType.COUNT_BOOSTER) {
                return com.example.examplemod.spawner.SpawnerModuleConfig.MAX_UPGRADE_STACK_SIZE;
            }
            return 1; // 其他模块不能堆叠
        }

        public com.example.examplemod.spawner.SpawnerModuleType getAllowedType() {
            return allowedType;
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
