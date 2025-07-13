package com.example.examplemod.fluid;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Optional;

/**
 * 经验流体 - 用于存储击杀怪物获得的经验
 * 特点：发光、粘稠、可以形成源方块、向上浮动
 */
public abstract class ExperienceFluid extends FlowingFluid {

    @Override
    public Fluid getFlowing() {
        return ExampleMod.FLOWING_EXPERIENCE.get();
    }

    @Override
    public Fluid getSource() {
        return ExampleMod.EXPERIENCE.get();
    }

    @Override
    public Item getBucket() {
        return ExampleMod.EXPERIENCE_BUCKET.get();
    }

    @Override
    public boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
        // 经验流体可以形成源方块（类似水）
        return level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WATER_SOURCE_CONVERSION);
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        // 当方块被破坏时触发邻居更新
        if (levelAccessor instanceof ServerLevel serverLevel) {
            this.triggerNeighborUpdate(serverLevel, blockPos);
        }
    }

    // 触发邻居流体更新
    private void triggerNeighborUpdate(ServerLevel level, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    FluidState checkFluidState = level.getFluidState(checkPos);

                    if (checkFluidState.getType().isSame(this) && !checkFluidState.isSource()) {
                        level.scheduleTick(checkPos, checkFluidState.getType(), 1);
                    }
                }
            }
        }
    }

    @Override
    protected int getSlopeFindDistance(LevelReader levelReader) {
        return 4; // 流体寻找斜坡的距离
    }

    @Override
    protected int getDropOff(LevelReader levelReader) {
        return 1; // 每格递减1
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ExampleMod.EXPERIENCE.get() || fluid == ExampleMod.FLOWING_EXPERIENCE.get();
    }

    @Override
    protected BlockState createLegacyBlock(FluidState fluidState) {
        return ExampleMod.EXPERIENCE_BLOCK.get().defaultBlockState()
            .setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.EXPERIENCE_ORB_PICKUP);
    }

    @Override
    public int getTickDelay(LevelReader levelReader) {
        return 10; // 比水慢一些的流动速度
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public FluidType getFluidType() {
        return ExampleMod.EXPERIENCE_TYPE.get();
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (!fluidState.isSource()) {
            FluidState newFluidState = this.getNewLiquid(level, pos, level.getBlockState(pos));
            int spreadDelay = this.getSpreadDelay(level, pos, fluidState, newFluidState);
            
            if (newFluidState.isEmpty()) {
                fluidState = newFluidState;
                blockState = Blocks.AIR.defaultBlockState();
                level.setBlock(pos, blockState, 3);
            } else if (newFluidState != fluidState) {
                fluidState = newFluidState;
                blockState = newFluidState.createLegacyBlock();
                level.setBlock(pos, blockState, 3);
                level.scheduleTick(pos, newFluidState.getType(), spreadDelay);
            }
        }

        this.spread(level, pos, blockState, fluidState);
    }

    @Override
    protected void randomTick(ServerLevel level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
        // 经验流体的随机tick - 可以添加特殊效果
        super.randomTick(level, blockPos, fluidState, randomSource);
    }

    // 静态源流体类
    public static class Source extends ExperienceFluid {
        @Override
        protected boolean canConvertToSource(ServerLevel serverLevel) {
            return false;
        }

        @Override
        public int getAmount(FluidState fluidState) {
            return 8;
        }

        @Override
        protected boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockGetter, BlockPos blockPos, Fluid fluid, Direction direction) {
            return false;
        }

        @Override
        public boolean isSource(FluidState fluidState) {
            return true;
        }
    }

    // 流动流体类
    public static class Flowing extends ExperienceFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockGetter, BlockPos blockPos, Fluid fluid, Direction direction) {
            return false;
        }

        @Override
        protected boolean canConvertToSource(ServerLevel serverLevel) {
            return false;
        }

        @Override
        public int getAmount(FluidState fluidState) {
            return fluidState.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState fluidState) {
            return false;
        }
    }
}
