package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.SpawnAreaRenderer;
import com.example.examplemod.client.TransparentItemRenderer;
import com.example.examplemod.client.SlotHintManager;
import com.example.examplemod.spawner.SpawnerModuleType;
import com.example.examplemod.redstone.RedstoneMode;
import com.example.examplemod.network.RedstoneModeUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class SpawnEggMobSpawnerScreen extends AbstractContainerScreen<SpawnEggMobSpawnerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/spawn_egg_mob_spawner.png");
    private Button showAreaButton;
    // 位置调整按钮
    private Button xMinusButton, xPlusButton;
    private Button yMinusButton, yPlusButton;
    private Button zMinusButton, zPlusButton;
    // 红石控制按钮
    private Button redstoneButton;
    private static boolean showSpawnArea = false;
    private int lastKnownRange = -1;
    private int currentRange = 4;
    // 当前偏移位置
    private int offsetX = 0, offsetY = 0, offsetZ = 0;

    public SpawnEggMobSpawnerScreen(SpawnEggMobSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 212; // 进一步增加高度来容纳8个模块槽位和位置调整按钮
    }

    @Override
    protected void init() {
        super.init();

        // 初始化槽位提示系统
        SlotHintManager.initializeHints();

        // 检查当前是否正在渲染这个刷怪器的区域
        showSpawnArea = SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos());

        // 获取当前刷怪范围和偏移
        currentRange = this.menu.getSpawnRange();
        offsetX = this.menu.getOffsetX();
        offsetY = this.menu.getOffsetY();
        offsetZ = this.menu.getOffsetZ();

        // 添加显示/隐藏刷怪区域的按钮
        this.showAreaButton = Button.builder(
            Component.translatable(showSpawnArea ? "gui.examplemod.hide_area" : "gui.examplemod.show_area"),
            this::toggleSpawnArea
        ).bounds(this.leftPos + 100, this.topPos + 55, 70, 20).build();

        this.addRenderableWidget(this.showAreaButton);

        // 添加位置调整按钮（移动到模块槽位下方）
        int buttonY = this.topPos + 95; // 向下移动到模块槽位下方
        int buttonSize = 20;
        int spacing = 25;

        // X轴调整按钮
        this.xMinusButton = Button.builder(Component.literal("X-"), (btn) -> adjustOffset(-1, 0, 0))
            .bounds(this.leftPos + 8, buttonY, buttonSize, buttonSize).build();
        this.xPlusButton = Button.builder(Component.literal("X+"), (btn) -> adjustOffset(1, 0, 0))
            .bounds(this.leftPos + 8 + spacing, buttonY, buttonSize, buttonSize).build();

        // Y轴调整按钮
        this.yMinusButton = Button.builder(Component.literal("Y-"), (btn) -> adjustOffset(0, -1, 0))
            .bounds(this.leftPos + 8 + spacing * 2, buttonY, buttonSize, buttonSize).build();
        this.yPlusButton = Button.builder(Component.literal("Y+"), (btn) -> adjustOffset(0, 1, 0))
            .bounds(this.leftPos + 8 + spacing * 3, buttonY, buttonSize, buttonSize).build();

        // Z轴调整按钮
        this.zMinusButton = Button.builder(Component.literal("Z-"), (btn) -> adjustOffset(0, 0, -1))
            .bounds(this.leftPos + 8 + spacing * 4, buttonY, buttonSize, buttonSize).build();
        this.zPlusButton = Button.builder(Component.literal("Z+"), (btn) -> adjustOffset(0, 0, 1))
            .bounds(this.leftPos + 8 + spacing * 5, buttonY, buttonSize, buttonSize).build();

        this.addRenderableWidget(this.xMinusButton);
        this.addRenderableWidget(this.xPlusButton);
        this.addRenderableWidget(this.yMinusButton);
        this.addRenderableWidget(this.yPlusButton);
        this.addRenderableWidget(this.zMinusButton);
        this.addRenderableWidget(this.zPlusButton);

        // 添加红石控制按钮（移动到右上方）
        RedstoneMode currentMode = getCurrentRedstoneMode();
        this.redstoneButton = Button.builder(currentMode.getLocalizedName(), (btn) -> toggleRedstoneMode())
            .bounds(this.leftPos + 120, this.topPos + 20, 50, 16).build();
        this.addRenderableWidget(this.redstoneButton);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 定期更新红石按钮状态，确保与服务器同步
        updateRedstoneButton();
        // 更新偏移显示
        updateOffsetDisplay();
    }

    // 缓存上次的红石模式，避免频繁更新
    private RedstoneMode lastRedstoneMode = null;

    /**
     * 更新红石按钮的显示状态
     */
    private void updateRedstoneButton() {
        if (this.redstoneButton != null) {
            RedstoneMode currentMode = getCurrentRedstoneMode();
            // 只有当模式真正改变时才更新按钮
            if (lastRedstoneMode != currentMode) {
                this.redstoneButton.setMessage(currentMode.getLocalizedName());
                lastRedstoneMode = currentMode;
                System.out.println("SpawnEggMobSpawnerScreen: Button updated to " + currentMode.getDisplayName());
            }
        }
    }

    /**
     * 更新偏移显示
     */
    private void updateOffsetDisplay() {
        // 从菜单获取最新的偏移值
        int newOffsetX = this.menu.getOffsetX();
        int newOffsetY = this.menu.getOffsetY();
        int newOffsetZ = this.menu.getOffsetZ();

        // 如果偏移值发生变化，更新本地缓存
        if (newOffsetX != offsetX || newOffsetY != offsetY || newOffsetZ != offsetZ) {
            offsetX = newOffsetX;
            offsetY = newOffsetY;
            offsetZ = newOffsetZ;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 渲染槽位工具提示（方法已简化，避免API兼容性问题）
        renderSlotTooltips(guiGraphics, mouseX, mouseY);

        // 渲染槽位半透明提示
        renderSlotHints(guiGraphics);

        // 检查刷怪范围是否改变，如果改变则更新渲染和显示
        int menuRange = this.menu.getSpawnRange();

        // 计算增强后的范围
        int effectiveRange = menuRange;
        if (this.menu.getLevel() != null && this.menu.getLevel().isLoaded(this.menu.getBlockPos())) {
            var blockEntity = this.menu.getLevel().getBlockEntity(this.menu.getBlockPos());
            if (blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity spawner) {
                var moduleManager = spawner.getModuleManager();
                var baseStats = new com.example.examplemod.spawner.SpawnerModuleManager.SpawnerStats(
                    spawner.getSpawnRange(), spawner.getMinSpawnDelay(), spawner.getMaxSpawnDelay(),
                    spawner.getSpawnCount(), spawner.getMaxNearbyEntities(), spawner.getRequiredPlayerRange()
                );
                var enhancedStats = moduleManager.applyModules(baseStats);
                effectiveRange = enhancedStats.spawnRange();
            }
        }

        if (effectiveRange != lastKnownRange) {
            currentRange = effectiveRange;
            lastKnownRange = effectiveRange;

            if (SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos())) {
                SpawnAreaRenderer.updateSpawnRange(this.menu.getBlockPos(), effectiveRange);
                System.out.println("SpawnEggMobSpawnerScreen: Updated render range to " + effectiveRange + " (base: " + menuRange + ")");
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // 添加说明文字
        Component spawnEggText = Component.translatable("gui.examplemod.spawn_egg");
        guiGraphics.drawString(this.font, spawnEggText, 8, 25, 4210752, false);

        Component instructionText = Component.translatable("gui.examplemod.spawn_egg_instruction");
        guiGraphics.drawString(this.font, instructionText, 8, 55, 0x666666, false);

        // 显示当前偏移位置（移动到按钮上方）
        Component offsetText = Component.translatable("gui.examplemod.offset", offsetX, offsetY, offsetZ);
        guiGraphics.drawString(this.font, offsetText, 8, 85, 4210752, false);

        // 显示红石控制模式（移动到右上方按钮下方）
        RedstoneMode currentMode = getCurrentRedstoneMode();
        Component redstoneText = Component.translatable("gui.examplemod.redstone_mode", currentMode.getLocalizedName().getString());
        guiGraphics.drawString(this.font, redstoneText, 120, 38, 4210752, false);

        // 显示模块槽位标签
        Component moduleText = Component.translatable("gui.examplemod.modules");
        guiGraphics.drawString(this.font, moduleText, 8, 50, 4210752, false);

        // 显示每个槽位的模块类型标签
        String[] slotLabels = {
            "R-", "R+", "MD",  // 第一行：范围缩减、范围扩展、最小延迟
            "XD", "C+", "PI"   // 第二行：最大延迟、数量增强、玩家忽略
        };

        for (int i = 0; i < 6; i++) {
            int x = 8 + (i % 3) * 18 + 1;  // 槽位位置 + 1像素偏移（3列布局）
            int y = 55 + (i / 3) * 18 - 8; // 槽位上方8像素，对应新的槽位位置
            guiGraphics.drawString(this.font, slotLabels[i], x, y, 0x666666, false);
        }

        // 显示当前模块效果（如果有的话）
        if (this.menu.getLevel() != null && this.menu.getLevel().isLoaded(this.menu.getBlockPos())) {
            var blockEntity = this.menu.getLevel().getBlockEntity(this.menu.getBlockPos());
            if (blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity spawner) {
                var moduleManager = spawner.getModuleManager();
                var moduleInfo = moduleManager.getInstalledModulesInfo();

                int yOffset = 0;
                for (String info : moduleInfo) {
                    Component infoText = Component.literal("§7" + info);
                    guiGraphics.drawString(this.font, infoText, 70, 50 + yOffset, 0x666666, false);
                    yOffset += 10;
                }
            }
        }
    }

    private void toggleSpawnArea(Button button) {
        showSpawnArea = !showSpawnArea;
        button.setMessage(Component.translatable(showSpawnArea ? "gui.examplemod.hide_area" : "gui.examplemod.show_area"));

        if (showSpawnArea) {
            // 获取增强后的刷怪范围
            int effectiveRange = this.menu.getSpawnRange(); // 基础范围

            // 如果能获取到BlockEntity，计算增强后的范围
            if (this.menu.getLevel() != null && this.menu.getLevel().isLoaded(this.menu.getBlockPos())) {
                var blockEntity = this.menu.getLevel().getBlockEntity(this.menu.getBlockPos());
                if (blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity spawner) {
                    var moduleManager = spawner.getModuleManager();
                    var baseStats = new com.example.examplemod.spawner.SpawnerModuleManager.SpawnerStats(
                        spawner.getSpawnRange(), spawner.getMinSpawnDelay(), spawner.getMaxSpawnDelay(),
                        spawner.getSpawnCount(), spawner.getMaxNearbyEntities(), spawner.getRequiredPlayerRange()
                    );
                    var enhancedStats = moduleManager.applyModules(baseStats);
                    effectiveRange = enhancedStats.spawnRange();
                    System.out.println("SpawnEggMobSpawnerScreen: Using enhanced range " + effectiveRange + " (base: " + spawner.getSpawnRange() + ")");
                }
            }

            // 启动区域渲染，使用增强后的刷怪范围和偏移
            SpawnAreaRenderer.startRendering(this.menu.getBlockPos(), effectiveRange,
                                            offsetX, offsetY, offsetZ);
        } else {
            // 停止区域渲染
            SpawnAreaRenderer.stopRendering();
        }
    }

    private void adjustOffset(int deltaX, int deltaY, int deltaZ) {
        // 限制偏移范围在合理区间内
        int newOffsetX = Math.max(-16, Math.min(16, offsetX + deltaX));
        int newOffsetY = Math.max(-16, Math.min(16, offsetY + deltaY));
        int newOffsetZ = Math.max(-16, Math.min(16, offsetZ + deltaZ));

        // 只有在值真正改变时才更新
        if (newOffsetX != offsetX || newOffsetY != offsetY || newOffsetZ != offsetZ) {
            offsetX = newOffsetX;
            offsetY = newOffsetY;
            offsetZ = newOffsetZ;

            updateSpawnOffset();
        }
    }

    private void updateSpawnOffset() {
        // 创建并发送偏移更新包
        com.example.examplemod.network.SpawnOffsetUpdatePacket packet =
            new com.example.examplemod.network.SpawnOffsetUpdatePacket(
                this.menu.getBlockPos(),
                offsetX,
                offsetY,
                offsetZ
            );
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);

        // 如果当前正在显示区域，立即更新渲染偏移
        if (SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos())) {
            SpawnAreaRenderer.updateSpawnOffset(this.menu.getBlockPos(), offsetX, offsetY, offsetZ);
        }

        System.out.println("SpawnEggMobSpawnerScreen: Updated spawn offset to X=" + offsetX + " Y=" + offsetY + " Z=" + offsetZ);
    }

    private void renderSlotTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 工具提示功能暂时禁用，等待API兼容性修复
        // 槽位标签已经提供了足够的信息指导用户
    }

    /**
     * 渲染槽位半透明提示
     */
    private void renderSlotHints(GuiGraphics guiGraphics) {
        // 定义每个槽位允许的模块类型
        SpawnerModuleType[] slotTypes = {
            SpawnerModuleType.RANGE_REDUCER,    // 槽位0
            SpawnerModuleType.RANGE_EXPANDER,   // 槽位1
            SpawnerModuleType.MIN_DELAY_REDUCER, // 槽位2
            SpawnerModuleType.MAX_DELAY_REDUCER, // 槽位3
            SpawnerModuleType.COUNT_BOOSTER,    // 槽位4
            SpawnerModuleType.PLAYER_IGNORER    // 槽位5
        };

        // 获取当前游戏时间用于脉动效果
        long gameTime = System.currentTimeMillis();

        // 为每个空槽位渲染半透明提示
        for (int i = 0; i < 6; i++) {
            // 计算槽位的屏幕坐标
            int slotX = this.leftPos + 8 + (i % 3) * 18;
            int slotY = this.topPos + 55 + (i / 3) * 18;

            // 检查槽位是否为空
            if (this.menu.slots.size() > i + 1) { // +1 因为第一个槽位是刷怪蛋
                var slot = this.menu.slots.get(i + 1);
                if (slot.getItem().isEmpty()) {
                    // 获取对应的提示物品
                    ItemStack hintItem = SlotHintManager.getHintItem(slotTypes[i]);
                    if (!hintItem.isEmpty()) {
                        // 渲染简单的半透明提示（使用兼容的方法）
                        TransparentItemRenderer.renderSimpleTransparentItem(
                            guiGraphics, hintItem, slotX, slotY
                        );
                    }
                }
            }
        }
    }

    /**
     * 获取当前红石模式
     */
    private RedstoneMode getCurrentRedstoneMode() {
        if (this.menu.getLevel() != null && this.menu.getLevel().isLoaded(this.menu.getBlockPos())) {
            var blockEntity = this.menu.getLevel().getBlockEntity(this.menu.getBlockPos());
            if (blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity spawner) {
                return spawner.getRedstoneMode();
            }
        }
        return RedstoneMode.ALWAYS; // 默认模式
    }

    /**
     * 切换红石控制模式
     */
    private void toggleRedstoneMode() {
        RedstoneMode currentMode = getCurrentRedstoneMode();
        RedstoneMode nextMode = currentMode.getNext();

        // 发送网络包到服务器
        PacketDistributor.sendToServer(new RedstoneModeUpdatePacket(this.menu.getBlockPos(), nextMode));

        // 不做乐观更新，等待服务端确认
        // 界面会通过 containerTick() -> updateRedstoneButton() 自动更新

        System.out.println("SpawnEggMobSpawnerScreen: Requesting redstone mode change to " + nextMode.getDisplayName());
    }

    @Override
    public void removed() {
        super.removed();
        // 关闭界面时不自动停止渲染，让用户可以在关闭界面后继续看到区域
        // 用户需要手动点击 "Hide Area" 按钮来停止渲染
    }
}
