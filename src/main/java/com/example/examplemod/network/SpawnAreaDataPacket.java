package com.example.examplemod.network;

import com.example.examplemod.client.SpawnAreaRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpawnAreaDataPacket(
    BlockPos pos,
    int spawnRange
) implements CustomPacketPayload {

    public static final Type<SpawnAreaDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "spawn_area_data"));

    public static final StreamCodec<FriendlyByteBuf, SpawnAreaDataPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SpawnAreaDataPacket::pos,
        ByteBufCodecs.INT, SpawnAreaDataPacket::spawnRange,
        SpawnAreaDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpawnAreaDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 客户端处理：更新渲染器的范围信息
            if (context.flow().isClientbound()) {
                SpawnAreaRenderer.updateSpawnRange(packet.pos(), packet.spawnRange());
            }
        });
    }
}
