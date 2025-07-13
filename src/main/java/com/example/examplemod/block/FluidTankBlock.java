package com.example.examplemod.block;

import com.example.examplemod.blockentity.FluidTankBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 流体储罐方块
 * 支持NeoForge流体API，可以存储和传输流体
 */
public class FluidTankBlock extends BaseEntityBlock {
    
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    
    // 储罐的碰撞箱 (稍微小一点，便于管道连接)
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    
    public FluidTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FluidTankBlockEntity tankEntity) {
                // 打开GUI或显示流体信息
                if (player instanceof ServerPlayer serverPlayer) {
                    // 这里可以打开GUI，暂时先在聊天中显示信息
                    ((ServerPlayer) player).sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Fluid Tank: " + tankEntity.getFluidAmount() + "/" + tankEntity.getCapacity() + " mB"
                    ));
                    
                    if (!tankEntity.getFluidStack().isEmpty()) {
                        ((ServerPlayer) player).sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Fluid: " + tankEntity.getFluidStack().getFluid().getFluidType().getDescription().getString()
                        ));
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
//
//    @Override
//    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
//        if (!state.is(newState.getBlock())) {
//            BlockEntity blockEntity = level.getBlockEntity(pos);
//            if (blockEntity instanceof FluidTankBlockEntity tankEntity) {
//                // 可以在这里处理储罐被破坏时的流体掉落
//                // 暂时不掉落流体，避免复杂性
//            }
//        }
//        super.onRemove(state, level, pos, newState, isMoving);
//    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (!level.isClientSide) {
            return createTickerHelper(blockEntityType, com.example.examplemod.ExampleMod.FLUID_TANK_BLOCK_ENTITY.get(),
                FluidTankBlockEntity::serverTick);
        }
        return null;
    }
}
