package com.example.examplemod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 客户端事件处理器
 * 负责处理客户端特定的事件，如定期检查渲染状态
 */
@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {
    
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // 每20 ticks（1秒）检查一次
    // TODO 这种实现不是很好，之后看看能不能优化
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tickCounter++;
        
        // 每秒检查一次渲染状态
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            // 检查SpawnAreaRenderer是否需要停止渲染
            checkSpawnAreaRenderer();
        }
    }
    
    private static void checkSpawnAreaRenderer() {
        // 如果正在渲染但方块不存在，停止渲染
        if (SpawnAreaRenderer.isRendering()) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var spawnerPos = SpawnAreaRenderer.getSpawnerPos();
            
            if (spawnerPos != null && minecraft.level != null) {
                if (!minecraft.level.isLoaded(spawnerPos)) {
                    // 区块未加载，停止渲染
                    SpawnAreaRenderer.stopRendering();
                    System.out.println("ClientEventHandler: Chunk unloaded, stopping spawn area render");
                    return;
                }
                
                var blockEntity = minecraft.level.getBlockEntity(spawnerPos);
                if (!(blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity)) {
                    // 方块不存在或不是刷怪器，停止渲染
                    SpawnAreaRenderer.stopRendering();
                    System.out.println("ClientEventHandler: Spawner block removed, stopping spawn area render");
                }
            }
        }
    }
}
