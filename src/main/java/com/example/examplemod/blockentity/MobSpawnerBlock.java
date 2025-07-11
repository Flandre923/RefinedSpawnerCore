package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MobSpawnerBlock extends BaseEntityBlock {

    public MobSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobSpawnerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        
        return createTickerHelper(blockEntityType, ExampleMod.MOB_SPAWNER_BLOCK_ENTITY.get(), MobSpawnerBlockEntity::tick);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MobSpawnerBlockEntity) {
            // 可以根据生成器状态返回红石信号强度
            return 0;
        }
        return 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MobSpawnerBlockEntity spawner && player instanceof ServerPlayer serverPlayer) {
                // 空手打开刷怪蛋界面
                spawner.openSpawnEggMenu(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MobSpawnerBlockEntity spawner && player instanceof ServerPlayer serverPlayer) {
                // 检查是否是木棍
                if (stack.is(net.minecraft.world.item.Items.STICK)) {
                    // 木棍打开调试界面
                    serverPlayer.openMenu(spawner, (buf) -> {
                        buf.writeBlockPos(pos);
                        // 发送当前的所有数据值到客户端
                        buf.writeInt(spawner.getSpawnDelay());
                        buf.writeInt(spawner.getMinSpawnDelay());
                        buf.writeInt(spawner.getMaxSpawnDelay());
                        buf.writeInt(spawner.getSpawnCount());
                        buf.writeInt(spawner.getMaxNearbyEntities());
                        buf.writeInt(spawner.getRequiredPlayerRange());
                        buf.writeInt(spawner.getSpawnRange());
                    });
                    return InteractionResult.SUCCESS;
                } else {
                    // 其他物品也打开刷怪蛋界面
                    spawner.openSpawnEggMenu(serverPlayer);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
