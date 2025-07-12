package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.SpawnAreaRenderer;
import com.example.examplemod.network.MobSpawnerUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class MobSpawnerScreen extends AbstractContainerScreen<MobSpawnerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/mob_spawner.png");
    
    private EditBox spawnRangeBox;
    private EditBox maxNearbyEntitiesBox;
    private EditBox spawnCountBox;
    private EditBox minSpawnDelayBox;
    private EditBox maxSpawnDelayBox;
    private EditBox requiredPlayerRangeBox;
    private Button showAreaButton;
    private static boolean showSpawnArea = false;

    public MobSpawnerScreen(MobSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        
        int x = this.leftPos + 8;
        int y = this.topPos + 20;
        int spacing = 22;

        // 生成范围
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.spawn_range"), (button) -> {})
                .bounds(x, y, 80, 20).build());
        this.spawnRangeBox = new EditBox(this.font, x + 85, y, 40, 20, Component.literal(""));
        this.spawnRangeBox.setValue(String.valueOf(this.menu.getSpawnRange()));
        this.spawnRangeBox.setMaxLength(2);
        this.spawnRangeBox.setResponder(this::onSpawnRangeChanged); // 添加实时响应
        this.addRenderableWidget(this.spawnRangeBox);

        y += spacing;

        // 最大附近实体数量
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.max_nearby_entities"), (button) -> {})
                .bounds(x, y, 80, 20).build());
        this.maxNearbyEntitiesBox = new EditBox(this.font, x + 85, y, 40, 20, Component.literal(""));
        this.maxNearbyEntitiesBox.setValue(String.valueOf(this.menu.getMaxNearbyEntities()));
        this.maxNearbyEntitiesBox.setMaxLength(2);
        this.addRenderableWidget(this.maxNearbyEntitiesBox);

        y += spacing;

        // 生成数量
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.spawn_count"), (button) -> {})
                .bounds(x, y, 80, 20).build());
        this.spawnCountBox = new EditBox(this.font, x + 85, y, 40, 20, Component.literal(""));
        this.spawnCountBox.setValue(String.valueOf(this.menu.getSpawnCount()));
        this.spawnCountBox.setMaxLength(2);
        this.addRenderableWidget(this.spawnCountBox);

        y += spacing;

        // 最小生成延迟
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.min_spawn_delay"), (button) -> {})
                .bounds(x, y, 80, 20).build());
        this.minSpawnDelayBox = new EditBox(this.font, x + 85, y, 40, 20, Component.literal(""));
        this.minSpawnDelayBox.setValue(String.valueOf(this.menu.getMinSpawnDelay()));
        this.minSpawnDelayBox.setMaxLength(4);
        this.addRenderableWidget(this.minSpawnDelayBox);

        y += spacing;

        // 最大生成延迟
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.max_spawn_delay"), (button) -> {})
                .bounds(x, y, 80, 20).build());
        this.maxSpawnDelayBox = new EditBox(this.font, x + 85, y, 40, 20, Component.literal(""));
        this.maxSpawnDelayBox.setValue(String.valueOf(this.menu.getMaxSpawnDelay()));
        this.maxSpawnDelayBox.setMaxLength(4);
        this.addRenderableWidget(this.maxSpawnDelayBox);

        y += spacing;

        // 玩家激活范围
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.required_player_range"), (button) -> {})
                .bounds(x, y, 80, 20).build());
        this.requiredPlayerRangeBox = new EditBox(this.font, x + 85, y, 40, 20, Component.literal(""));
        this.requiredPlayerRangeBox.setValue(String.valueOf(this.menu.getRequiredPlayerRange()));
        this.requiredPlayerRangeBox.setMaxLength(2);
        this.addRenderableWidget(this.requiredPlayerRangeBox);

        // 应用按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.apply"), this::applyChanges)
                .bounds(this.leftPos + 130, this.topPos + 140, 40, 20).build());

        // 检查当前是否正在渲染这个刷怪器的区域
        showSpawnArea = SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos());

        // 显示/隐藏刷怪区域按钮
        this.showAreaButton = Button.builder(
            Component.translatable(showSpawnArea ? "gui.examplemod.hide_area" : "gui.examplemod.show_area"),
            this::toggleSpawnArea
        ).bounds(this.leftPos + 8, this.topPos + 140, 70, 20).build();

        this.addRenderableWidget(this.showAreaButton);
    }

    private void applyChanges(Button button) {
        try {
            int spawnRange = Integer.parseInt(this.spawnRangeBox.getValue());
            int maxNearbyEntities = Integer.parseInt(this.maxNearbyEntitiesBox.getValue());
            int spawnCount = Integer.parseInt(this.spawnCountBox.getValue());
            int minSpawnDelay = Integer.parseInt(this.minSpawnDelayBox.getValue());
            int maxSpawnDelay = Integer.parseInt(this.maxSpawnDelayBox.getValue());
            int requiredPlayerRange = Integer.parseInt(this.requiredPlayerRangeBox.getValue());

            // 验证数值范围
            spawnRange = Math.max(1, Math.min(16, spawnRange));
            maxNearbyEntities = Math.max(1, Math.min(32, maxNearbyEntities));
            spawnCount = Math.max(1, Math.min(16, spawnCount));
            minSpawnDelay = Math.max(1, Math.min(9999, minSpawnDelay));
            maxSpawnDelay = Math.max(minSpawnDelay, Math.min(9999, maxSpawnDelay));
            requiredPlayerRange = Math.max(1, Math.min(64, requiredPlayerRange));

            // 发送到服务端
            MobSpawnerUpdatePacket packet = new MobSpawnerUpdatePacket(
                this.menu.getBlockPos(),
                spawnRange,
                maxNearbyEntities,
                spawnCount,
                minSpawnDelay,
                maxSpawnDelay,
                requiredPlayerRange
            );
            PacketDistributor.sendToServer(packet);

            // 如果当前正在显示刷怪区域，立即更新渲染范围
            if (SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos())) {
                SpawnAreaRenderer.updateSpawnRange(this.menu.getBlockPos(), spawnRange);
                System.out.println("MobSpawnerScreen: Updated render range to " + spawnRange);
            }

        } catch (NumberFormatException e) {
            // 处理无效输入
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    private void toggleSpawnArea(Button button) {
        showSpawnArea = !showSpawnArea;
        button.setMessage(Component.translatable(showSpawnArea ? "gui.examplemod.hide_area" : "gui.examplemod.show_area"));

        if (showSpawnArea) {
            // 启动区域渲染，使用实际的刷怪范围
            SpawnAreaRenderer.startRendering(this.menu.getBlockPos(), this.menu.getSpawnRange());
        } else {
            // 停止区域渲染
            SpawnAreaRenderer.stopRendering();
        }
    }

    private void onSpawnRangeChanged(String newValue) {
        // 当刷怪范围输入框内容改变时，实时更新渲染
        if (SpawnAreaRenderer.isRenderingAt(this.menu.getBlockPos())) {
            try {
                int newRange = Integer.parseInt(newValue);
                newRange = Math.max(1, Math.min(16, newRange)); // 验证范围
                SpawnAreaRenderer.updateSpawnRange(this.menu.getBlockPos(), newRange);
                System.out.println("MobSpawnerScreen: Real-time updated render range to " + newRange);
            } catch (NumberFormatException e) {
                // 忽略无效输入，等待用户完成输入
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        // 关闭界面时不自动停止渲染，让用户可以在关闭界面后继续看到区域
        // 用户需要手动点击 "Hide Area" 按钮来停止渲染
    }
}
