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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
        // 当方块被破坏时（包括源方块），主动触发邻居流体的重新检查
        if (levelAccessor instanceof ServerLevel serverLevel) {
            this.triggerNeighborUpdate(serverLevel, blockPos);
        }
    }


    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
        // 如果是移除源方块（变成空气），触发邻居更新
        if (blockState.getFluidState().isSource() && fluidState.isEmpty()) {
            if (level instanceof ServerLevel serverLevel) {
                this.triggerNeighborUpdate(serverLevel, pos);
            }
        }
        super.spreadTo(level, pos, blockState, direction, fluidState);
    }

    // 主动触发邻居流体的更新检查
    private void triggerNeighborUpdate(ServerLevel level, BlockPos pos) {

        // 更激进的方法：在更大范围内强制所有流体重新检查
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    FluidState checkFluidState = level.getFluidState(checkPos);

                    if (checkFluidState.getType().isSame(this) && !checkFluidState.isSource()) {
                        // 强制所有非源流体立即重新检查
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
        return 1; // 基础递减量，但向上流动时会被特殊处理以保持更高强度
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

    protected void randomTick(ServerLevel level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
        // 在随机tick中强制检查消散
        if (!fluidState.isSource()) {
            // 强制重新调度正常tick
            level.scheduleTick(blockPos, fluidState.getType(), 1);
        }
    }

    public int getTickDelay(LevelReader levelReader) {
        return 5; // 基础tick延迟
    }

    // 重写getSpreadDelay来控制消散速度
    @Override
    protected int getSpreadDelay(Level level, BlockPos pos, FluidState currentState, FluidState newState) {
        // 如果是消散过程（新状态强度更低），使用更长的延迟
        if (!newState.isEmpty() && newState.getAmount() < currentState.getAmount()) {
            return this.getTickDelay(level) * 3; // 消散时延迟更长，更自然
        }
        return this.getTickDelay(level);
    }

    // 重写这个方法确保流体总是被调度
    @Override
    public boolean isRandomlyTicking() {
        return true; // 强制流体进行随机tick
    }

    @Override
    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
        int maxStrength = 0;
        // 检查所有邻居（包括下方）来确定流体状态
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                FluidState neighborFluidState = neighborState.getFluidState();

                // 检查下方是否有同类型流体并且可以穿透
                if (neighborFluidState.getType().isSame(this) && this.canPassThroughWall(direction, level, pos, state, neighborPos, neighborState)) {
                    // 从下方获得的强度（向上流动的推力），增强 1，上限 7
                    // 这是确保它能向上流动并维持柱子的关键
                    int strengthFromNeighbor = Math.min(7, neighborFluidState.getAmount() + 1);
                    maxStrength = Math.max(maxStrength, strengthFromNeighbor);
                }
            } else if (direction.getAxis().isHorizontal()) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                FluidState neighborFluidState = neighborState.getFluidState();

                if (neighborFluidState.getType().isSame(this) && this.canPassThroughWall(direction, level, pos, state, neighborPos, neighborState)) {
                    maxStrength = Math.max(maxStrength, neighborFluidState.getAmount());
                }
            }
        }

        int finalStrength = maxStrength - this.getDropOff(level);

        if (finalStrength <= 0) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            // 强制限制最大强度为7，防止创建源方块
            int safeFinalStrength = Math.min(finalStrength, 7);
            return this.getFlowing(safeFinalStrength, false); // 确保不是falling状态
        }
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public FluidType getFluidType() {
        return ExampleMod.MAGIC_WATER_TYPE.get();
    }

    // 重写tick方法，防止向下流动并确保持续更新
    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {

        if (!fluidState.isSource()) {
            FluidState newFluidState = this.getNewLiquid(level, pos, level.getBlockState(pos));
            int spreadDelay = this.getSpreadDelay(level, pos, fluidState, newFluidState);
            if (newFluidState.isEmpty()) {
                // 流体消散时，通知邻居重新检查
                this.notifyNeighborsOfChange(level, pos);
                fluidState = newFluidState;
                blockState = Blocks.AIR.defaultBlockState();
                level.setBlock(pos, blockState, 3);
            } else if (newFluidState != fluidState) {
                fluidState = newFluidState;
                blockState = newFluidState.createLegacyBlock();
                level.setBlock(pos, blockState, 3);
                level.scheduleTick(pos, newFluidState.getType(), spreadDelay);
            } else {
                level.scheduleTick(pos, fluidState.getType(), this.getTickDelay(level)); // 更频繁的检查
            }
        }

        // 只执行向上流动的spread逻辑
        this.spread(level, pos, blockState, fluidState);
    }

    // 通知邻居流体重新检查状态
    private void notifyNeighborsOfChange(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluidState = level.getFluidState(neighborPos);

            if (neighborFluidState.getType().isSame(this) && !neighborFluidState.isSource()) {
                // 强制邻居流体重新调度tick
                level.scheduleTick(neighborPos, neighborFluidState.getType(), 1);
            }
        }
    }

    // 重写spread方法实现正确的优先级逻辑
    @Override
    protected void spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (!fluidState.isEmpty()) {
            // 优先尝试向上流动
            if (this.canFlowUpward(level, pos, blockState, fluidState)) {
                this.flowUpward(level, pos, blockState, fluidState);
                return; // 向上流动成功，不需要侧面扩散
            }

            // 只有当无法向上流动时（被阻挡），才进行侧面扩散
            this.spreadToSidesWhenBlocked(level, pos, fluidState, blockState);
        }
    }

    // 检查是否可以向上流动 - 非常宽松的条件让流体流动更远
    private boolean canFlowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        // 几乎所有强度的流体都能向上流动
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

        // 计算向上流动的强度，增强向上流动能力
        int currentStrength = fluidState.getAmount();
        // 向上流动时强度增强，让流体能流动很远
        int upwardStrength = Math.min(7, Math.max(2, currentStrength + 1)); // 增强强度

        // 如果强度太低，不进行向上流动
        if (upwardStrength <= 0) {
            return;
        }

        // 确保不创建源方块，即使强度为8
        int safeUpwardStrength = Math.min(upwardStrength, 7);
        FluidState newFluidState = this.getFlowing(safeUpwardStrength, false); // 确保不是falling状态

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

    // 只有在被阻挡时才向侧面扩散
    private void spreadToSidesWhenBlocked(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState blockState) {
        // 确认上方确实被阻挡
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.isAir() || aboveState.canBeReplaced()) {
            // 上方有空间，不应该侧面扩散
            return;
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
                    // 向侧面流动时保持较高强度，确保能继续向上流动
                    int newStrength = Math.max(2, flowStrength - 1); // 保持至少强度2
                    // 确保不创建源方块
                    int safeNewStrength = Math.min(newStrength, 7);
                    FluidState newFluidState = this.getFlowing(safeNewStrength, false);


                    if (this.canHoldSpecificFluid(level, neighborPos, neighborState, newFluidState.getType())) {
                        if (neighborFluidState.canBeReplacedWith(level, neighborPos, newFluidState.getType(), direction)) {
                            this.spreadTo(level, neighborPos, neighborState, direction, newFluidState);

                            // 检查侧面位置是否可以继续向上流动
                            BlockPos sideAbovePos = neighborPos.above();
                            BlockState sideAboveState = level.getBlockState(sideAbovePos);
                            if ((sideAboveState.isAir() || sideAboveState.canBeReplaced()) && safeNewStrength >= 2) {
                                // 如果侧面位置上方有空间且强度足够，调度向上流动（稍微延迟避免抖动）
                                level.scheduleTick(neighborPos, newFluidState.getType(), this.getTickDelay(level) + 2);
                            } else {
                                // 如果不能向上流动，调度正常tick继续侧面扩散
                                level.scheduleTick(neighborPos, newFluidState.getType(), this.getTickDelay(level) * 2);
                            }
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
                // 确保不创建源方块
                int safeUpwardStrength = Math.min(upwardStrength, 7);
                return this.getFlowing(safeUpwardStrength, false);
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

        // 魔法水不应该自动生成源方块！
        // 禁用源方块生成逻辑
        /*
        if (sourceCount >= 2) {
            return this.getSource(false);
        }
        */

        // 否则基于水平邻居的强度创建流动流体
        int finalStrength = Math.max(1, maxHorizontalStrength - this.getDropOff(level));
        if (finalStrength <= 0) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            // 确保不创建源方块
            int safeFinalStrength = Math.min(finalStrength, 7);
            return this.getFlowing(safeFinalStrength, false);
        }
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

    // 检查范围内是否有任何源方块
    private boolean hasAnySourceInRange(ServerLevel level, BlockPos pos, int range) {
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    FluidState checkFluidState = level.getFluidState(checkPos);

                    if (checkFluidState.getType().isSame(this) &&
                        checkFluidState.isSource() &&
                        checkFluidState.getAmount() == 8) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 检查流体是否正在活跃流动（更宽松的检测避免抖动）
    private boolean isActivelyFlowing(ServerLevel level, BlockPos pos, int currentStrength) {
        // 检查下方是否有流体支持向上流动（放宽条件）
        BlockPos belowPos = pos.below();
        FluidState belowFluidState = level.getFluidState(belowPos);

        if (belowFluidState.getType().isSame(this) && !belowFluidState.isEmpty()) {
            // 放宽条件：下方有同类型流体且强度相近就认为是活跃流动
            if (belowFluidState.getAmount() >= currentStrength - 1) { // 允许稍弱的支持
                return true;
            }
        }

        // 检查是否有水平方向的流体支持（也放宽条件）
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluidState = level.getFluidState(neighborPos);

            if (neighborFluidState.getType().isSame(this) && !neighborFluidState.isEmpty()) {
                // 放宽条件：邻居强度相近就认为是活跃流动
                if (neighborFluidState.getAmount() >= currentStrength - 1) {
                    return true;
                }
            }
        }

        // 特殊检查：如果上方有空间且当前强度足够，也认为是活跃流动
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if ((aboveState.isAir() || aboveState.canBeReplaced()) && currentStrength >= 2) {
            return true;
        }

        return false;
    }
}
