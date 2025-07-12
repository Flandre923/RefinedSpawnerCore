package com.example.examplemod.network;

import com.example.examplemod.blockentity.MobSpawnerBlockEntity;
import com.example.examplemod.redstone.RedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 红石模式更新网络包
 * 用于客户端向服务器发送红石模式变更请求
 */
public record RedstoneModeUpdatePacket(BlockPos pos, RedstoneMode mode) implements CustomPacketPayload {
    
    public static final Type<RedstoneModeUpdatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "redstone_mode_update"));
    
    public static final StreamCodec<FriendlyByteBuf, RedstoneModeUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        RedstoneModeUpdatePacket::pos,
        StreamCodec.of(
            (buf, mode) -> buf.writeUtf(mode.getSerializedName()),
            buf -> RedstoneMode.fromString(buf.readUtf())
        ),
        RedstoneModeUpdatePacket::mode,
        RedstoneModeUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理服务器端接收到的红石模式更新包
     */
    public static void handleServer(RedstoneModeUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            
            // 验证玩家权限和距离
            if (player.distanceToSqr(packet.pos.getX(), packet.pos.getY(), packet.pos.getZ()) > 64) {
                return; // 距离太远，忽略请求
            }
            
            // 获取方块实体并更新红石模式
            if (player.level().isLoaded(packet.pos)) {
                var blockEntity = player.level().getBlockEntity(packet.pos);
                if (blockEntity instanceof MobSpawnerBlockEntity spawner) {
                    spawner.setRedstoneMode(packet.mode);
                    System.out.println("RedstoneModeUpdatePacket: Updated redstone mode to " + packet.mode.getDisplayName() + " at " + packet.pos);
                }
            }
        });
    }
}
