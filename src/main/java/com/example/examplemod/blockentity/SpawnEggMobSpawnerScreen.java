package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.SpawnAreaRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpawnEggMobSpawnerScreen extends AbstractContainerScreen<SpawnEggMobSpawnerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/spawn_egg_mob_spawner.png");
    private Button showAreaButton;
    // 位置调整按钮
    private Button xMinusButton, xPlusButton;
    private Button yMinusButton, yPlusButton;
    private Button zMinusButton, zPlusButton;
    private static boolean showSpawnArea = false;
    private int lastKnownRange = -1;
    private int currentRange = 4;
    // 当前偏移位置
    private int offsetX = 0, offsetY = 0, offsetZ = 0;

    public SpawnEggMobSpawnerScreen(SpawnEggMobSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // 检查当前是否正在渲染这个刷怪器的区域
        showSpawnArea = SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos());

        // 获取当前刷怪范围和偏移
        currentRange = this.menu.getSpawnRange();
        offsetX = this.menu.getOffsetX();
        offsetY = this.menu.getOffsetY();
        offsetZ = this.menu.getOffsetZ();

        // 添加显示/隐藏刷怪区域的按钮
        this.showAreaButton = Button.builder(
            Component.literal(showSpawnArea ? "Hide Area" : "Show Area"),
            this::toggleSpawnArea
        ).bounds(this.leftPos + 100, this.topPos + 55, 70, 20).build();

        this.addRenderableWidget(this.showAreaButton);

        // 添加位置调整按钮
        int buttonY = this.topPos + 80;
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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 检查刷怪范围是否改变，如果改变则更新渲染和显示
        int menuRange = this.menu.getSpawnRange();
        if (menuRange != lastKnownRange) {
            currentRange = menuRange;
            lastKnownRange = menuRange;

            if (SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos())) {
                SpawnAreaRenderer.updateSpawnRange(this.menu.getBlockPos(), currentRange);
                System.out.println("SpawnEggMobSpawnerScreen: Updated render range to " + currentRange);
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
        Component spawnEggText = Component.literal("Spawn Egg:");
        guiGraphics.drawString(this.font, spawnEggText, 8, 25, 4210752, false);

        Component instructionText = Component.literal("Place a spawn egg to specify mob type");
        guiGraphics.drawString(this.font, instructionText, 8, 55, 0x666666, false);

        // 显示当前偏移位置
        Component offsetText = Component.literal("Offset: X=" + offsetX + " Y=" + offsetY + " Z=" + offsetZ);
        guiGraphics.drawString(this.font, offsetText, 8, 70, 4210752, false);
    }

    private void toggleSpawnArea(Button button) {
        showSpawnArea = !showSpawnArea;
        button.setMessage(Component.literal(showSpawnArea ? "Hide Area" : "Show Area"));

        if (showSpawnArea) {
            // 启动区域渲染，使用实际的刷怪范围和偏移
            SpawnAreaRenderer.startRendering(this.menu.getBlockPos(), this.menu.getSpawnRange(),
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

    @Override
    public void removed() {
        super.removed();
        // 关闭界面时不自动停止渲染，让用户可以在关闭界面后继续看到区域
        // 用户需要手动点击 "Hide Area" 按钮来停止渲染
    }
}
