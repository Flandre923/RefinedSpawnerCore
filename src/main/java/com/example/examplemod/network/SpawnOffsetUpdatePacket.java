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

public record SpawnOffsetUpdatePacket(
    BlockPos pos,
    int offsetX,
    int offsetY,
    int offsetZ
) implements CustomPacketPayload {

    public static final Type<SpawnOffsetUpdatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "spawn_offset_update"));

    public static final StreamCodec<FriendlyByteBuf, SpawnOffsetUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SpawnOffsetUpdatePacket::pos,
        ByteBufCodecs.INT, SpawnOffsetUpdatePacket::offsetX,
        ByteBufCodecs.INT, SpawnOffsetUpdatePacket::offsetY,
        ByteBufCodecs.INT, SpawnOffsetUpdatePacket::offsetZ,
        SpawnOffsetUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpawnOffsetUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && player.level().isLoaded(packet.pos())) {
                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
                if (blockEntity instanceof MobSpawnerBlockEntity spawner) {
                    // 验证玩家是否有权限修改（距离检查）
                    if (player.distanceToSqr(packet.pos().getX() + 0.5, packet.pos().getY() + 0.5, packet.pos().getZ() + 0.5) <= 64) {
                        // 验证偏移范围并应用
                        int offsetX = Math.max(-16, Math.min(16, packet.offsetX()));
                        int offsetY = Math.max(-16, Math.min(16, packet.offsetY()));
                        int offsetZ = Math.max(-16, Math.min(16, packet.offsetZ()));

                        spawner.setOffsetX(offsetX);
                        spawner.setOffsetY(offsetY);
                        spawner.setOffsetZ(offsetZ);

                        spawner.setChanged();
                    }
                }
            }
        });
    }
}
