package com.example.examplemod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 简化的半透明物品渲染器
 * 使用兼容的API实现半透明效果
 */
@OnlyIn(Dist.CLIENT)
public class SimpleTransparentItemRenderer {
    
    /**
     * 渲染半透明的物品图标
     * 使用覆盖层方法实现视觉上的半透明效果
     */
    public static void renderTransparentItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, float alpha) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 渲染物品本身
        guiGraphics.renderItem(itemStack, x, y);
        
        // 根据透明度覆盖半透明层来模拟透明效果
        if (alpha < 1.0f) {
            int overlayAlpha = (int)((1.0f - alpha) * 160); // 计算覆盖层透明度
            int overlayColor = (overlayAlpha << 24) | 0xC0C0C0; // 灰色半透明覆盖
            guiGraphics.fill(x, y, x + 16, y + 16, overlayColor);
        }
    }
    
    /**
     * 渲染带脉动效果的槽位提示
     */
    public static void renderSlotHint(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if (itemStack.isEmpty()) {
            return;
        }

        // 获取当前时间用于脉动效果
        long gameTime = System.currentTimeMillis();
        
        // 计算脉动透明度 (0.1f 到 0.3f 之间脉动，更低的透明度)
        float pulseAlpha = 0.1f + 0.2f * (float)(Math.sin(gameTime * 0.003) * 0.5 + 0.5);
        
        // 渲染脉动边框
        int pulseIntensity = (int)(pulseAlpha * 100);
        int borderColor = (pulseIntensity << 24) | 0xFFFFFF; // 脉动白色边框
        
        // 绘制边框
        guiGraphics.fill(x - 1, y - 1, x + 17, y, borderColor); // 上边框
        guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, borderColor); // 下边框
        guiGraphics.fill(x - 1, y, x, y + 16, borderColor); // 左边框
        guiGraphics.fill(x + 16, y, x + 17, y + 16, borderColor); // 右边框
        
        // 渲染半透明物品
        renderTransparentItem(guiGraphics, itemStack, x, y, pulseAlpha);
    }
    
    /**
     * 渲染带彩色边框的槽位提示
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
        renderTransparentItem(guiGraphics, itemStack, x, y, 0.2f);
    }
    
    /**
     * 简单的半透明渲染（兼容版本）
     */
    public static void renderSimpleTransparentItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        renderTransparentItem(guiGraphics, itemStack, x, y, 0.2f);  // 调整到20%透明度
    }
}
