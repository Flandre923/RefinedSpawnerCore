package com.example.examplemod.network;

import com.example.examplemod.blockentity.MobSpawnerBlockEntity;
import com.example.examplemod.redstone.RedstoneMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 红石模式客户端同步网络包
 * 用于服务器向客户端同步红石模式状态
 */
public record RedstoneModeClientSyncPacket(BlockPos pos, RedstoneMode mode) implements CustomPacketPayload {
    
    public static final Type<RedstoneModeClientSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "redstone_mode_client_sync"));
    
    public static final StreamCodec<FriendlyByteBuf, RedstoneModeClientSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        RedstoneModeClientSyncPacket::pos,
        StreamCodec.of(
            (buf, mode) -> buf.writeUtf(mode.getSerializedName()),
            buf -> RedstoneMode.fromString(buf.readUtf())
        ),
        RedstoneModeClientSyncPacket::mode,
        RedstoneModeClientSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理客户端接收到的红石模式同步包
     */
    public static void handleClient(RedstoneModeClientSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 确保在客户端执行
            if (context.flow().isClientbound()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level != null && minecraft.level.isLoaded(packet.pos)) {
                    var blockEntity = minecraft.level.getBlockEntity(packet.pos);
                    if (blockEntity instanceof MobSpawnerBlockEntity spawner) {
                        spawner.setRedstoneModeClient(packet.mode);
                        System.out.println("RedstoneModeClientSyncPacket: Client updated redstone mode to " + packet.mode.getDisplayName() + " at " + packet.pos);
                    }
                }
            }
        });
    }
}
