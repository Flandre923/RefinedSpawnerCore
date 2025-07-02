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
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    // 重写这个方法来防止向下流动逻辑
    @Override
    public boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
        return false; // 魔法水不能转换为源方块
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
        return 1; // 保持标准的递减量，但在向上流动时会特殊处理
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

    // 重写getNewLiquid方法，确保正确处理流体消散
    @Override
    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
        int maxStrength = 0;
        int sourceCount = 0;
        boolean hasFluidNeighbor = false;

        // 检查所有邻居（包括下方）来确定流体状态
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluidState = neighborState.getFluidState();

            if (neighborFluidState.getType().isSame(this) && this.canPassThroughWall(direction, level, pos, state, neighborPos, neighborState)) {
                hasFluidNeighbor = true;

                if (neighborFluidState.isSource()) {
                    sourceCount++;
                }

                // 计算从这个方向获得的强度
                int strengthFromNeighbor;
                if (direction == Direction.DOWN) {
                    // 从下方获得的强度（向上流动的推力）
                    strengthFromNeighbor = Math.max(1, neighborFluidState.getAmount() - 1);
                } else if (direction == Direction.UP) {
                    // 从上方获得的强度（向上流动的延续）
                    strengthFromNeighbor = Math.min(8, neighborFluidState.getAmount() + 1);
                } else {
                    // 从水平方向获得的强度
                    strengthFromNeighbor = Math.max(1, neighborFluidState.getAmount() - this.getDropOff(level));
                }

                maxStrength = Math.max(maxStrength, strengthFromNeighbor);
            }
        }

        // 如果没有任何流体邻居，这个位置应该是空的
        if (!hasFluidNeighbor) {
            return Fluids.EMPTY.defaultFluidState();
        }

        // 如果有足够的源方块，创建源方块
        if (sourceCount >= 2) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            FluidState belowFluidState = belowState.getFluidState();
            if (belowState.isSolid() || this.isSourceBlockOfThisType(belowFluidState)) {
                return this.getSource(false);
            }
        }

        // 创建流动流体，如果强度太低则返回空
        return maxStrength <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(maxStrength, false);
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public FluidType getFluidType() {
        return ExampleMod.MAGIC_WATER_TYPE.get();
    }

    // 重写tick方法，防止向下流动
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

        // 只执行向上流动的spread逻辑
        this.spread(level, pos, blockState, fluidState);
    }

    // 重写spread方法实现向上流动优先逻辑
    @Override
    protected void spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (!fluidState.isEmpty()) {
            // 检查是否可以向上流动
            if (this.canFlowUpward(level, pos, blockState, fluidState)) {
                // 向上流动
                this.flowUpward(level, pos, blockState, fluidState);
                return; // 向上流动成功，不进行侧面扩散
            }

            // 只有在无法向上流动时才考虑侧面扩散
            // 并且不受下方方块影响
            this.spreadToSidesOnly(level, pos, fluidState, blockState);
        }
    }

    // 检查是否可以向上流动 - 调整条件让流体能流动更远
    private boolean canFlowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        // 降低强度阈值，让流体能流动更远
        if (fluidState.getAmount() < 1) {
            return false;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState aboveFluidState = aboveState.getFluidState();

        // 检查上方是否完全空闲或可替换
        if (!aboveState.isAir() && !aboveState.canBeReplaced() && !aboveFluidState.isEmpty()) {
            // 如果上方有流体，检查是否是相同类型且强度更低
            if (aboveFluidState.getType().isSame(this)) {
                return aboveFluidState.getAmount() < fluidState.getAmount();
            }
            return false; // 上方有其他类型的流体或不可替换的方块
        }

        // 检查是否可以通过和容纳流体
        return this.canMaybePassThrough(level, pos, blockState, Direction.UP, abovePos, aboveState, aboveFluidState) &&
               this.canHoldSpecificFluid(level, abovePos, aboveState, fluidState.getType());
    }

    // 执行向上流动 - 更严格的控制
    private void flowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState aboveFluidState = aboveState.getFluidState();

        // 计算向上流动的强度，减少递减让流体流动更远
        int currentStrength = fluidState.getAmount();
        // 向上流动时强度递减更少，让流体能流动更远
        int upwardStrength = Math.max(1, currentStrength - 1);

        // 如果强度太低，不进行向上流动
        if (upwardStrength <= 0) {
            return;
        }

        FluidState newFluidState = this.getFlowing(upwardStrength, false); // 确保不是falling状态

        // 更严格的替换检查
        boolean canReplace = false;
        if (aboveState.isAir()) {
            canReplace = true;
        } else if (!aboveFluidState.isEmpty() && aboveFluidState.getType().isSame(this)) {
            // 只有当上方流体强度更低时才替换
            canReplace = aboveFluidState.getAmount() < upwardStrength;
        } else if (aboveState.canBeReplaced()) {
            canReplace = true;
        }

        if (canReplace && this.canHoldSpecificFluid(level, abovePos, aboveState, newFluidState.getType())) {
            this.spreadTo(level, abovePos, aboveState, Direction.UP, newFluidState);

            // 安排下一次tick，继续向上流动
            level.scheduleTick(abovePos, newFluidState.getType(), this.getTickDelay(level));
        }
    }

    // 只向侧面扩散，仅在确实无法向上流动时执行
    private void spreadToSidesOnly(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState blockState) {
        // 再次确认真的无法向上流动
        if (this.canFlowUpward(level, pos, blockState, fluidState)) {
            return; // 如果还能向上流动，就不要向侧面扩散
        }

        // 检查上方是否真的被阻挡
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.isAir() || aboveState.canBeReplaced()) {
            return; // 上方还有空间，不应该向侧面扩散
        }

        int flowStrength = fluidState.getAmount() - this.getDropOff(level);

        // 魔法水不应该有FALLING状态，但为了安全起见检查一下
        if (fluidState.getValue(FALLING)) {
            // 如果意外标记为falling，重置为正常状态
            flowStrength = Math.max(1, fluidState.getAmount() - 1);
        }

        if (flowStrength > 0) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                FluidState neighborFluidState = neighborState.getFluidState();

                if (this.canMaybePassThrough(level, pos, blockState, direction, neighborPos, neighborState, neighborFluidState)) {
                    // 向侧面流动时强度显著递减
                    int newStrength = Math.max(1, flowStrength - 3);
                    FluidState newFluidState = this.getFlowing(newStrength, false);

                    if (this.canHoldSpecificFluid(level, neighborPos, neighborState, newFluidState.getType())) {
                        if (neighborFluidState.canBeReplacedWith(level, neighborPos, newFluidState.getType(), direction)) {
                            this.spreadTo(level, neighborPos, neighborState, direction, newFluidState);
                        }
                    }
                }
            }
        }
    }

    // 为向上流动创建新的流体状态 - 专注于向上传递能量
    protected FluidState getNewLiquidForUpwardFlow(ServerLevel level, BlockPos pos, BlockState state) {
        // 检查下方的流体作为向上流动的源头
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        FluidState belowFluidState = belowState.getFluidState();

        // 如果下方有相同类型的流体，从下方获取能量向上流动
        if (!belowFluidState.isEmpty() && belowFluidState.getType().isSame(this)) {
            if (this.canPassThroughWall(Direction.DOWN, level, pos, state, belowPos, belowState)) {
                // 向上流动时保持较强的流动力
                int upwardStrength = Math.max(1, belowFluidState.getAmount() - 1);
                return this.getFlowing(upwardStrength, false);
            }
        }

        // 检查水平邻居作为备用能量源
        int maxHorizontalStrength = 0;
        int sourceCount = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluidState = neighborState.getFluidState();

            if (neighborFluidState.getType().isSame(this) && this.canPassThroughWall(direction, level, pos, state, neighborPos, neighborState)) {
                if (neighborFluidState.isSource()) {
                    sourceCount++;
                }
                maxHorizontalStrength = Math.max(maxHorizontalStrength, neighborFluidState.getAmount());
            }
        }

        // 如果有足够的源方块，创建源方块
        if (sourceCount >= 2) {
            return this.getSource(false);
        }

        // 否则基于水平邻居的强度创建流动流体
        int finalStrength = Math.max(1, maxHorizontalStrength - this.getDropOff(level));
        return finalStrength <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(finalStrength, false);
    }

    // 辅助方法：检查是否可以通过墙壁
    private boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos pos, BlockState state, BlockPos spreadPos, BlockState spreadState) {
        VoxelShape voxelshape = spreadState.getCollisionShape(level, spreadPos);
        if (voxelshape == Shapes.block()) {
            return false;
        } else {
            VoxelShape voxelshape1 = state.getCollisionShape(level, pos);
            if (voxelshape1 == Shapes.block()) {
                return false;
            } else if (voxelshape1 == Shapes.empty() && voxelshape == Shapes.empty()) {
                return true;
            } else {
                return !Shapes.mergedFaceOccludes(voxelshape1, voxelshape, direction);
            }
        }
    }

    // 辅助方法：检查是否可以通过
    private boolean canMaybePassThrough(BlockGetter level, BlockPos pos, BlockState state, Direction direction, BlockPos spreadPos, BlockState spreadState, FluidState fluidState) {
        return !this.isSourceBlockOfThisType(fluidState) && this.canHoldAnyFluid(spreadState) && this.canPassThroughWall(direction, level, pos, state, spreadPos, spreadState);
    }

    // 辅助方法：检查是否是此类型的源方块
    private boolean isSourceBlockOfThisType(FluidState state) {
        return state.getType().isSame(this) && state.isSource();
    }

    // 辅助方法：检查是否可以容纳任何流体
    private boolean canHoldAnyFluid(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer) {
            return true;
        } else {
            return !state.blocksMotion() && !state.is(Blocks.LADDER) && !state.is(Blocks.SUGAR_CANE);
        }
    }

    // 辅助方法：检查是否可以容纳特定流体
    private boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer liquidblockcontainer) {
            return liquidblockcontainer.canPlaceLiquid(null, level, pos, state, fluid);
        } else {
            return true;
        }
    }

    // 辅助方法：计算源邻居数量
    private int sourceNeighborCount(LevelReader level, BlockPos pos) {
        int i = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.relative(direction);
            FluidState fluidstate = level.getFluidState(blockpos);
            if (this.isSourceBlockOfThisType(fluidstate)) {
                ++i;
            }
        }
        return i;
    }

    // 旧的spreadToSides方法已被spreadToSidesOnly替代

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

    // 自定义的流体洞检测方法，替代不可访问的isWaterHole方法
    private boolean isFluidHole(BlockGetter level, BlockPos pos, BlockState state, BlockPos belowPos, BlockState belowState) {
        if (!this.canPassThroughWall(Direction.DOWN, level, pos, state, belowPos, belowState)) {
            return false;
        } else {
            return belowState.getFluidState().getType().isSame(this) ? true : this.canHoldFluid(level, belowPos, belowState, this.getFlowing());
        }
    }

    // 自定义的流体容纳检测方法
    private boolean canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return this.canHoldAnyFluid(state) && this.canHoldSpecificFluid(level, pos, state, fluid);
    }
}
