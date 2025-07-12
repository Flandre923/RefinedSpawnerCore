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
                        // 验证数值范围并应用（-1表示不更改该字段）
                        if (packet.spawnRange() >= 0) {
                            int spawnRange = Math.max(1, Math.min(16, packet.spawnRange()));
                            spawner.setSpawnRange(spawnRange);
                        }

                        if (packet.maxNearbyEntities() >= 0) {
                            int maxNearbyEntities = Math.max(1, Math.min(32, packet.maxNearbyEntities()));
                            spawner.setMaxNearbyEntities(maxNearbyEntities);
                        }

                        if (packet.spawnCount() >= 0) {
                            int spawnCount = Math.max(1, Math.min(16, packet.spawnCount()));
                            spawner.setSpawnCount(spawnCount);
                        }

                        if (packet.minSpawnDelay() >= 0) {
                            int minSpawnDelay = Math.max(1, Math.min(9999, packet.minSpawnDelay()));
                            spawner.setMinSpawnDelay(minSpawnDelay);
                        }

                        if (packet.maxSpawnDelay() >= 0) {
                            int maxSpawnDelay = Math.max(packet.minSpawnDelay() >= 0 ? Math.max(1, Math.min(9999, packet.minSpawnDelay())) : spawner.getMinSpawnDelay(),
                                                        Math.min(9999, packet.maxSpawnDelay()));
                            spawner.setMaxSpawnDelay(maxSpawnDelay);
                        }

                        if (packet.requiredPlayerRange() >= 0) {
                            int requiredPlayerRange = Math.max(1, Math.min(64, packet.requiredPlayerRange()));
                            spawner.setRequiredPlayerRange(requiredPlayerRange);
                        }

                        spawner.setChanged();
                    }
                }
            }
        });
    }
}
