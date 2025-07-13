package com.example.examplemod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 半透明物品渲染器
 * 用于在GUI中渲染半透明的物品图标，作为槽位提示
 */
@OnlyIn(Dist.CLIENT)
public class TransparentItemRenderer {
    
    /**
     * 渲染半透明的物品图标
     *
     * @param guiGraphics GUI图形上下文
     * @param itemStack 要渲染的物品
     * @param x X坐标
     * @param y Y坐标
     * @param alpha 透明度 (0.0f = 完全透明, 1.0f = 完全不透明)
     */
    public static void renderTransparentItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, float alpha) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 使用简化的方法来模拟半透明效果
        // 通过在物品上覆盖半透明层来实现视觉上的透明效果

        // 渲染物品本身
        guiGraphics.renderItem(itemStack, x, y);

        // 根据透明度覆盖半透明层
        if (alpha < 1.0f) {
            int overlayAlpha = (int)((1.0f - alpha) * 180); // 计算覆盖层透明度
            int overlayColor = (overlayAlpha << 24) | 0xC0C0C0; // 灰色半透明覆盖
            guiGraphics.fill(x, y, x + 16, y + 16, overlayColor);
        }
    }
    
    /**
     * 渲染半透明的物品图标（默认透明度）
     * 
     * @param guiGraphics GUI图形上下文
     * @param itemStack 要渲染的物品
     * @param x X坐标
     * @param y Y坐标
     */
    public static void renderTransparentItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        renderTransparentItem(guiGraphics, itemStack, x, y, 0.3f); // 默认30%透明度
    }
    
    /**
     * 渲染半透明的物品图标，带有发光效果
     *
     * @param guiGraphics GUI图形上下文
     * @param itemStack 要渲染的物品
     * @param x X坐标
     * @param y Y坐标
     * @param alpha 透明度
     * @param glowIntensity 发光强度 (0.0f = 无发光, 1.0f = 最强发光)
     */
    public static void renderTransparentItemWithGlow(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, float alpha, float glowIntensity) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 简化的发光效果实现
        if (glowIntensity > 0.0f) {
            // 渲染发光层（简化版本）
            for (int i = 0; i < 2; i++) {
                int offset = i + 1;
                renderTransparentItem(guiGraphics, itemStack, x - offset, y, glowIntensity * 0.3f);
                renderTransparentItem(guiGraphics, itemStack, x + offset, y, glowIntensity * 0.3f);
                renderTransparentItem(guiGraphics, itemStack, x, y - offset, glowIntensity * 0.3f);
                renderTransparentItem(guiGraphics, itemStack, x, y + offset, glowIntensity * 0.3f);
            }
        }

        // 渲染主要物品
        renderTransparentItem(guiGraphics, itemStack, x, y, alpha);
    }
    
    /**
     * 渲染脉动效果的半透明物品图标
     * 
     * @param guiGraphics GUI图形上下文
     * @param itemStack 要渲染的物品
     * @param x X坐标
     * @param y Y坐标
     * @param gameTime 游戏时间（用于计算脉动效果）
     */
    public static void renderPulsingTransparentItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, long gameTime) {
        if (itemStack.isEmpty()) {
            return;
        }
        
        // 计算脉动透明度 (0.2f 到 0.6f 之间脉动)
        float pulseAlpha = 0.2f + 0.4f * (float)(Math.sin(gameTime * 0.003) * 0.5 + 0.5);
        
        renderTransparentItem(guiGraphics, itemStack, x, y, pulseAlpha);
    }
    
    /**
     * 渲染带边框的半透明物品图标
     *
     * @param guiGraphics GUI图形上下文
     * @param itemStack 要渲染的物品
     * @param x X坐标
     * @param y Y坐标
     * @param alpha 透明度
     * @param borderColor 边框颜色 (ARGB格式)
     */
    public static void renderTransparentItemWithBorder(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, float alpha, int borderColor) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 渲染边框
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);

        // 渲染半透明物品
        renderTransparentItem(guiGraphics, itemStack, x, y, alpha);
    }

    /**
     * 渲染简单的半透明物品（兼容版本）
     * 这个方法使用半透明效果渲染槽位提示
     */
    public static void renderSimpleTransparentItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 使用默认的半透明度渲染
        renderTransparentItem(guiGraphics, itemStack, x, y, 0.4f);
    }

    /**
     * 渲染带脉动效果的半透明槽位提示
     * 这个方法提供更好的视觉反馈
     */
    public static void renderSlotHint(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 获取当前时间用于脉动效果
        long gameTime = System.currentTimeMillis();

        // 计算脉动透明度 (0.3f 到 0.6f 之间脉动)
        float pulseAlpha = 0.3f + 0.3f * (float)(Math.sin(gameTime * 0.003) * 0.5 + 0.5);

        // 渲染脉动背景边框
        int pulseIntensity = (int)(pulseAlpha * 128);
        int borderColor = (pulseIntensity << 24) | 0xFFFFFF; // 脉动白色边框
        guiGraphics.fill(x - 1, y - 1, x + 17, y, borderColor); // 上边框
        guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, borderColor); // 下边框
        guiGraphics.fill(x - 1, y, x, y + 16, borderColor); // 左边框
        guiGraphics.fill(x + 16, y, x + 17, y + 16, borderColor); // 右边框

        // 渲染半透明物品
        renderTransparentItem(guiGraphics, itemStack, x, y, pulseAlpha);
    }

    /**
     * 渲染带边框的半透明槽位提示
     * 提供更清晰的视觉指示
     */
    public static void renderSlotHintWithBorder(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, int borderColor) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 渲染彩色边框
        guiGraphics.fill(x - 1, y - 1, x + 17, y, borderColor); // 上边框
        guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, borderColor); // 下边框
        guiGraphics.fill(x - 1, y, x, y + 16, borderColor); // 左边框
        guiGraphics.fill(x + 16, y, x + 17, y + 16, borderColor); // 右边框

        // 渲染半透明背景
        guiGraphics.fill(x, y, x + 16, y + 16, 0x30000000); // 半透明黑色背景

        // 渲染半透明物品
        renderTransparentItem(guiGraphics, itemStack, x, y, 0.5f);
    }
}
