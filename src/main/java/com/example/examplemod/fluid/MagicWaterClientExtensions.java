package com.example.examplemod.fluid;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * 客户端流体扩展，用于定义流体的渲染属性
 */
public class MagicWaterClientExtensions implements IClientFluidTypeExtensions {
    
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "block/magic_water_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "block/magic_water_flow");
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "block/magic_water_overlay");

    @Override
    public ResourceLocation getStillTexture() {
        return STILL_TEXTURE;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING_TEXTURE;
    }

    @Override
    public ResourceLocation getOverlayTexture() {
        return OVERLAY_TEXTURE;
    }

    @Override
    public int getTintColor() {
        return 0xFF6A5ACD; // 紫色调
    }
}
