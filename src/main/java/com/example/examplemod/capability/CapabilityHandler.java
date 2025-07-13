package com.example.examplemod.capability;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.blockentity.FluidTankBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Capability注册处理器
 * 注册方块实体的各种能力
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public class CapabilityHandler {
    
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 注册流体储罐的流体处理能力
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ExampleMod.FLUID_TANK_BLOCK_ENTITY.get(),
            (blockEntity, side) -> blockEntity.getFluidHandler(side)
        );
    }
}
