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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Optional;

public abstract class MagicWaterFluid extends FlowingFluid {

    @Override
    public Fluid getFlowing() {
        return ExampleMod.FLOWING_MAGIC_WATER.get();
    }

    @Override
    public Fluid getSource() {
        return ExampleMod.MAGIC_WATER.get();
    }

    @Override
    public Item getBucket() {
        return ExampleMod.MAGIC_WATER_BUCKET.get();
    }

    protected boolean canConvertToSource(ServerLevel level) {
        return false; // 不能形成无限水源
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        // 流体破坏方块前的逻辑
    }

    @Override
    protected int getSlopeFindDistance(LevelReader levelReader) {
        return 4; // 流体寻找斜坡的距离
    }

    @Override
    protected int getDropOff(LevelReader levelReader) {
        return 1; // 流体每格的高度降低量
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ExampleMod.MAGIC_WATER.get() || fluid == ExampleMod.FLOWING_MAGIC_WATER.get();
    }

    @Override
    protected BlockState createLegacyBlock(FluidState fluidState) {
        return ExampleMod.MAGIC_WATER_BLOCK.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return false;
    }

    protected void randomTick(ServerLevel level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
        // 随机tick逻辑
    }

    public int getTickDelay(LevelReader levelReader) {
        return 5; // tick延迟
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public FluidType getFluidType() {
        return ExampleMod.MAGIC_WATER_TYPE.get();
    }

    // 静态流体类
    public static class Source extends MagicWaterFluid {
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
    public static class Flowing extends MagicWaterFluid {
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
