package com.example.examplemod.network;

import com.example.examplemod.blockentity.MobSpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MobSpawnerUpdatePacket(
    BlockPos pos,
    int spawnRange,
    int maxNearbyEntities,
    int spawnCount,
    int minSpawnDelay,
    int maxSpawnDelay,
    int requiredPlayerRange
) implements CustomPacketPayload {

    public static final Type<MobSpawnerUpdatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "mob_spawner_update"));

    public static final StreamCodec<FriendlyByteBuf, MobSpawnerUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, MobSpawnerUpdatePacket::pos,
            ByteBufCodecs.INT, MobSpawnerUpdatePacket::spawnRange,
            ByteBufCodecs.INT, MobSpawnerUpdatePacket::maxNearbyEntities,
            ByteBufCodecs.INT, MobSpawnerUpdatePacket::spawnCount,
            ByteBufCodecs.INT, MobSpawnerUpdatePacket::minSpawnDelay,
            ByteBufCodecs.INT, MobSpawnerUpdatePacket::maxSpawnDelay,
            ByteBufCodecs.INT, MobSpawnerUpdatePacket::requiredPlayerRange,
        MobSpawnerUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MobSpawnerUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && player.level().isLoaded(packet.pos())) {
                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
                if (blockEntity instanceof MobSpawnerBlockEntity spawner) {
                    // 验证玩家是否有权限修改（距离检查）
                    if (player.distanceToSqr(packet.pos().getX() + 0.5, packet.pos().getY() + 0.5, packet.pos().getZ() + 0.5) <= 64) {
                        // 验证数值范围并应用
                        int spawnRange = Math.max(1, Math.min(16, packet.spawnRange()));
                        int maxNearbyEntities = Math.max(1, Math.min(32, packet.maxNearbyEntities()));
                        int spawnCount = Math.max(1, Math.min(16, packet.spawnCount()));
                        int minSpawnDelay = Math.max(1, Math.min(9999, packet.minSpawnDelay()));
                        int maxSpawnDelay = Math.max(minSpawnDelay, Math.min(9999, packet.maxSpawnDelay()));
                        int requiredPlayerRange = Math.max(1, Math.min(64, packet.requiredPlayerRange()));

                        spawner.setSpawnRange(spawnRange);
                        spawner.setMaxNearbyEntities(maxNearbyEntities);
                        spawner.setSpawnCount(spawnCount);
                        spawner.setMinSpawnDelay(minSpawnDelay);
                        spawner.setMaxSpawnDelay(maxSpawnDelay);
                        spawner.setRequiredPlayerRange(requiredPlayerRange);

                        spawner.setChanged();
                    }
                }
            }
        });
    }
}
