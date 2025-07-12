package com.example.examplemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public class SpawnAreaRenderer {
    private static boolean isRendering = false;
    private static BlockPos spawnerPos = null;
    private static int spawnRange = 4; // 默认刷怪范围
    private static int offsetX = 0, offsetY = 0, offsetZ = 0; // 偏移位置
    private static boolean isRegistered = false;

    public static void startRendering(BlockPos pos) {
        startRendering(pos, 4); // 使用默认范围
    }

    public static void startRendering(BlockPos pos, int range) {
        startRendering(pos, range, 0, 0, 0);
    }

    public static void startRendering(BlockPos pos, int range, int offX, int offY, int offZ) {
        spawnerPos = pos;
        spawnRange = range;
        offsetX = offX;
        offsetY = offY;
        offsetZ = offZ;
        isRendering = true;
        System.out.println("SpawnAreaRenderer: Starting rendering at " + pos + " with range " + range + " offset(" + offX + "," + offY + "," + offZ + ")");
        // 事件处理器应该已经在ExampleMod中注册了
    }

    public static void stopRendering() {
        isRendering = false;
        spawnerPos = null;
        System.out.println("SpawnAreaRenderer: Stopping rendering");
        // 不取消注册事件，保持注册状态以便下次使用
    }

    public static void updateSpawnRange(BlockPos pos, int range) {
        if (isRendering && pos.equals(spawnerPos)) {
            spawnRange = range;
            System.out.println("SpawnAreaRenderer: Updated spawn range to " + range);
        }
    }

    public static void updateSpawnOffset(BlockPos pos, int offX, int offY, int offZ) {
        if (isRendering && pos.equals(spawnerPos)) {
            offsetX = offX;
            offsetY = offY;
            offsetZ = offZ;
            System.out.println("SpawnAreaRenderer: Updated offset to (" + offX + "," + offY + "," + offZ + ")");
        }
    }

    public static boolean isRenderingAt(BlockPos pos) {
        return isRendering && pos.equals(spawnerPos);
    }

    public static boolean isRendering() {
        return isRendering;
    }

    public static BlockPos getSpawnerPos() {
        return spawnerPos;
    }

    /**
     * 检查当前渲染的刷怪器方块是否仍然存在
     */
    private static boolean isSpawnerBlockValid() {
        if (!isRendering || spawnerPos == null) {
            return false;
        }

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.isLoaded(spawnerPos)) {
            return false;
        }

        var blockEntity = minecraft.level.getBlockEntity(spawnerPos);
        return blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity;
    }

    /**
     * 获取当前有效的刷怪范围（考虑模块增强）
     */
    private static int getEffectiveSpawnRange() {
        if (!isRendering || spawnerPos == null) {
            return spawnRange;
        }

        // 尝试从世界中获取刷怪器并计算增强后的范围
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.isLoaded(spawnerPos)) {
            var blockEntity = minecraft.level.getBlockEntity(spawnerPos);
            if (blockEntity instanceof com.example.examplemod.blockentity.MobSpawnerBlockEntity spawner) {
                var moduleManager = spawner.getModuleManager();
                var baseStats = new com.example.examplemod.spawner.SpawnerModuleManager.SpawnerStats(
                    spawner.getSpawnRange(), spawner.getMinSpawnDelay(), spawner.getMaxSpawnDelay(),
                    spawner.getSpawnCount(), spawner.getMaxNearbyEntities(), spawner.getRequiredPlayerRange()
                );
                var enhancedStats = moduleManager.applyModules(baseStats);
                return enhancedStats.spawnRange();
            }
        }

        return spawnRange; // 回退到存储的范围
    }
    
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterEntities event) {
        if (!isRendering || spawnerPos == null) {
            return;
        }

        // 检查刷怪器方块是否仍然存在
        if (!isSpawnerBlockValid()) {
            // 如果方块不存在，自动停止渲染
            stopRendering();
            System.out.println("SpawnAreaRenderer: Spawner block no longer exists, stopping render");
            return;
        }

        // 调试信息
        System.out.println("SpawnAreaRenderer: Rendering spawn area at " + spawnerPos + " with range " + spawnRange);
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        // 检查玩家是否在合理距离内
        double distanceToSpawner = mc.player.distanceToSqr(spawnerPos.getX() + 0.5, spawnerPos.getY() + 0.5, spawnerPos.getZ() + 0.5);
        if (distanceToSpawner > 64 * 64) { // 64格距离
            // 距离太远时自动停止渲染
            System.out.println("SpawnAreaRenderer: Player too far from spawner, stopping rendering");
            stopRendering();
            return;
        }
        
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        // 获取相机位置
        Vec3 cameraPos = event.getCamera().getPosition();
        
        poseStack.pushPose();
        
        // 移动到相对于相机的位置
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        // 渲染刷怪区域边界框
        renderSpawnArea(poseStack, bufferSource, spawnerPos, spawnRange);
        
        poseStack.popPose();
        
        bufferSource.endBatch();
    }
    
    private static void renderSpawnArea(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos center, int range) {
        // 获取实际有效的范围（考虑模块增强）
        int effectiveRange = getEffectiveSpawnRange();

        // 创建刷怪区域的AABB，考虑偏移和模块增强
        BlockPos actualCenter = center.offset(offsetX, offsetY, offsetZ);
        AABB spawnArea = new AABB(
            actualCenter.getX() - effectiveRange, actualCenter.getY() - effectiveRange, actualCenter.getZ() - effectiveRange,
            actualCenter.getX() + effectiveRange + 1, actualCenter.getY() + effectiveRange + 1, actualCenter.getZ() + effectiveRange + 1
        );

        System.out.println("SpawnAreaRenderer: Rendering with effective range " + effectiveRange + " (base: " + range + ")");

        // 使用更简单的渲染类型
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.LINES);

        // 渲染边界框线条
        renderBoxLines(
            poseStack,
            lineConsumer,
            spawnArea.minX, spawnArea.minY, spawnArea.minZ,
            spawnArea.maxX, spawnArea.maxY, spawnArea.maxZ,
            1.0f, 0.0f, 0.0f, 1.0f // 红色
        );

        System.out.println("SpawnAreaRenderer: Rendered box from " + spawnArea.minX + "," + spawnArea.minY + "," + spawnArea.minZ +
                          " to " + spawnArea.maxX + "," + spawnArea.maxY + "," + spawnArea.maxZ);
    }

    private static void renderBoxLines(PoseStack poseStack, VertexConsumer vertexConsumer,
                                      double minX, double minY, double minZ,
                                      double maxX, double maxY, double maxZ,
                                      float red, float green, float blue, float alpha) {
        var matrix = poseStack.last().pose();
        var normalMatrix = poseStack.last().normal();

        // 渲染12条边线（每条线需要2个顶点）
        // 底面4条边
        addLine(matrix, normalMatrix, vertexConsumer, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);

        // 顶面4条边
        addLine(matrix, normalMatrix, vertexConsumer, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);

        // 4条竖直边
        addLine(matrix, normalMatrix, vertexConsumer, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addLine(matrix, normalMatrix, vertexConsumer, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void addLine(org.joml.Matrix4f matrix, org.joml.Matrix3f normalMatrix, VertexConsumer vertexConsumer,
                               double x1, double y1, double z1, double x2, double y2, double z2,
                               float red, float green, float blue, float alpha) {
        vertexConsumer.addVertex(matrix, (float)x1, (float)y1, (float)z1)
                     .setColor(red, green, blue, alpha)
                     .setNormal(0.0f, 1.0f, 0.0f);
        vertexConsumer.addVertex(matrix, (float)x2, (float)y2, (float)z2)
                     .setColor(red, green, blue, alpha)
                     .setNormal(0.0f, 1.0f, 0.0f);
    }

}
