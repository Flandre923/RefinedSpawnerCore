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
    private static boolean showSpawnArea = false;
    private int lastKnownRange = -1;

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

        // 添加显示/隐藏刷怪区域的按钮
        this.showAreaButton = Button.builder(
            Component.literal(showSpawnArea ? "Hide Area" : "Show Area"),
            this::toggleSpawnArea
        ).bounds(this.leftPos + 100, this.topPos + 55, 70, 20).build();

        this.addRenderableWidget(this.showAreaButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 检查刷怪范围是否改变，如果改变则更新渲染
        if (SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos())) {
            int currentRange = this.menu.getSpawnRange();
            if (currentRange != lastKnownRange) {
                SpawnAreaRenderer.updateSpawnRange(this.menu.getBlockPos(), currentRange);
                lastKnownRange = currentRange;
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
    }

    private void toggleSpawnArea(Button button) {
        showSpawnArea = !showSpawnArea;
        button.setMessage(Component.literal(showSpawnArea ? "Hide Area" : "Show Area"));

        if (showSpawnArea) {
            // 启动区域渲染，使用实际的刷怪范围
            SpawnAreaRenderer.startRendering(this.menu.getBlockPos(), this.menu.getSpawnRange());
        } else {
            // 停止区域渲染
            SpawnAreaRenderer.stopRendering();
        }
    }

    @Override
    public void removed() {
        super.removed();
        // 关闭界面时不自动停止渲染，让用户可以在关闭界面后继续看到区域
        // 用户需要手动点击 "Hide Area" 按钮来停止渲染
    }
}
