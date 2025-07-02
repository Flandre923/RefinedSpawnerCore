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

    // 主动触发邻居流体的更新检查
    private void triggerNeighborUpdate(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluidState = level.getFluidState(neighborPos);

            if (neighborFluidState.getType().isSame(this)) {
                // 强制邻居流体立即重新检查状态
                level.scheduleTick(neighborPos, neighborFluidState.getType(), 1);

                // 递归触发更远的邻居（但限制深度）
                this.triggerNeighborUpdateRecursive(level, neighborPos, 1, 8);
            }
        }
    }

    // 递归触发邻居更新，但限制深度
    private void triggerNeighborUpdateRecursive(ServerLevel level, BlockPos pos, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluidState = level.getFluidState(neighborPos);

            if (neighborFluidState.getType().isSame(this) && !neighborFluidState.isSource()) {
                // 调度tick检查
                level.scheduleTick(neighborPos, neighborFluidState.getType(), depth);

                // 继续递归，但增加深度
                this.triggerNeighborUpdateRecursive(level, neighborPos, depth + 1, maxDepth);
            }
        }
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

    protected void randomTick(ServerLevel level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
        // 在随机tick中强制检查消散
        System.out.println("DEBUG: Random tick at " + blockPos + " - forcing dissipation check");
        if (!fluidState.isSource()) {
            // 强制重新调度正常tick
            level.scheduleTick(blockPos, fluidState.getType(), 1);
        }
    }

    public int getTickDelay(LevelReader levelReader) {
        return 5; // tick延迟
    }

    // 重写这个方法确保流体总是被调度
    @Override
    public boolean isRandomlyTicking() {
        return true; // 强制流体进行随机tick
    }

    // 重写getNewLiquid方法，实现智能消散机制
    @Override
    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
        int maxStrength = 0;
        int sourceCount = 0;
        boolean hasFluidNeighbor = false;
        boolean hasValidSource = false;

        // 检查所有邻居（包括下方）来确定流体状态
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluidState = neighborState.getFluidState();

            if (neighborFluidState.getType().isSame(this) && this.canPassThroughWall(direction, level, pos, state, neighborPos, neighborState)) {
                hasFluidNeighbor = true;

                if (neighborFluidState.isSource()) {
                    sourceCount++;
                    hasValidSource = true;
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

        // 简化的消散检查：使用距离而不是复杂的递归搜索
        if (!hasValidSource) {
            // 检查是否在合理的源头距离内
            boolean withinSourceRange = this.isWithinSourceRange(level, pos, 16); // 最大搜索16格
            System.out.println("DEBUG: Fluid at " + pos + " - hasValidSource: " + hasValidSource +
                             ", withinSourceRange: " + withinSourceRange + ", maxStrength: " + maxStrength);
            if (!withinSourceRange) {
                System.out.println("DEBUG: Dissipating fluid at " + pos + " - no source within range");
                return Fluids.EMPTY.defaultFluidState();
            }
        }

        // 魔法水不应该自动生成源方块！
        // 注释掉源方块生成逻辑，防止意外创建源方块
        /*
        if (sourceCount >= 2) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            FluidState belowFluidState = belowState.getFluidState();
            if (belowState.isSolid() || this.isSourceBlockOfThisType(belowFluidState)) {
                return this.getSource(false);
            }
        }
        */

        // 创建流动流体，如果强度太低则返回空
        // 关键修复：确保永远不创建源方块，即使强度为8
        FluidState result;
        if (maxStrength <= 0) {
            result = Fluids.EMPTY.defaultFluidState();
        } else {
            // 强制限制最大强度为7，防止创建源方块
            int safeStrength = Math.min(maxStrength, 7);
            result = this.getFlowing(safeStrength, false);
        }
        System.out.println("DEBUG: getNewLiquid result at " + pos + " - " +
                         (result.isEmpty() ? "EMPTY" : "FLOWING amount=" + result.getAmount() + ", isSource=" + result.isSource()));
        return result;
    }

    // 检查是否有到源头的有效连接（递归搜索，但有深度限制）
    private boolean hasSourceConnection(ServerLevel level, BlockPos pos, Set<BlockPos> visited) {
        // 防止无限递归和重复检查
        if (visited.contains(pos) || visited.size() > 32) {
            System.out.println("DEBUG: Search terminated at " + pos + " - " +
                             (visited.contains(pos) ? "already visited" : "depth limit reached"));
            return false;
        }
        visited.add(pos);

        System.out.println("DEBUG: Searching for source from " + pos + ", visited count: " + visited.size());

        // 检查所有邻居
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluidState = level.getFluidState(neighborPos);

            if (neighborFluidState.getType().isSame(this)) {
                // 更严格的源方块检查
                if (neighborFluidState.isSource()) {
                    // 额外验证：检查方块状态确认这真的是源方块
                    BlockState neighborBlockState = level.getBlockState(neighborPos);
                    boolean isRealSource = this.isRealSourceBlock(level, neighborPos, neighborBlockState, neighborFluidState);
                    System.out.println("DEBUG: Found potential source at " + neighborPos + " from " + pos +
                                     ", isRealSource: " + isRealSource + ", amount: " + neighborFluidState.getAmount());
                    if (isRealSource) {
                        return true;
                    }
                }

                // 如果是流动流体，递归检查它是否连接到源头
                if (!neighborFluidState.isEmpty() && !visited.contains(neighborPos)) {
                    if (this.hasSourceConnection(level, neighborPos, visited)) {
                        return true;
                    }
                }
            }
        }

        System.out.println("DEBUG: No source connection found from " + pos);
        return false;
    }

    // 验证是否是真正的源方块
    private boolean isRealSourceBlock(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        // 简化检查：只验证流体状态
        boolean isSource = fluidState.isSource() && fluidState.getAmount() == 8;
        System.out.println("DEBUG: Verifying source at " + pos + " - isSource: " + fluidState.isSource() +
                         ", amount: " + fluidState.getAmount() + ", result: " + isSource);
        return isSource;
    }

    // 检查是否在源头范围内（使用简单的距离检查）
    private boolean isWithinSourceRange(ServerLevel level, BlockPos pos, int maxRange) {
        // 在指定范围内搜索真正的源方块
        for (int x = -maxRange; x <= maxRange; x++) {
            for (int y = -maxRange; y <= maxRange; y++) {
                for (int z = -maxRange; z <= maxRange; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    FluidState checkFluidState = level.getFluidState(checkPos);

                    if (checkFluidState.getType().isSame(this) &&
                        checkFluidState.isSource() &&
                        checkFluidState.getAmount() == 8) {

                        // 找到真正的源方块，检查是否可达
                        if (this.isReachableFrom(level, pos, checkPos, maxRange)) {
                            System.out.println("DEBUG: Found reachable source at " + checkPos + " from " + pos);
                            return true;
                        }
                    }
                }
            }
        }
        System.out.println("DEBUG: No reachable source found within range " + maxRange + " from " + pos);
        return false;
    }

    // 检查从起点到源头是否可达（简化的路径检查）
    private boolean isReachableFrom(ServerLevel level, BlockPos from, BlockPos to, int maxSteps) {
        // 简单的曼哈顿距离检查
        int distance = Math.abs(from.getX() - to.getX()) +
                      Math.abs(from.getY() - to.getY()) +
                      Math.abs(from.getZ() - to.getZ());

        // 如果距离太远，认为不可达
        if (distance > maxSteps) {
            return false;
        }

        // 检查路径上是否有连续的流体
        return this.hasFluidPath(level, from, to);
    }

    // 检查两点之间是否有流体路径
    private boolean hasFluidPath(ServerLevel level, BlockPos from, BlockPos to) {
        // 简化：只检查垂直路径（因为我们主要关心向上流动）
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
            int minY = Math.min(from.getY(), to.getY());
            int maxY = Math.max(from.getY(), to.getY());

            for (int y = minY; y <= maxY; y++) {
                BlockPos checkPos = new BlockPos(from.getX(), y, from.getZ());
                FluidState checkFluidState = level.getFluidState(checkPos);

                if (!checkFluidState.getType().isSame(this)) {
                    return false; // 路径中断
                }
            }
            return true;
        }

        // 对于非垂直路径，暂时返回true（可以后续优化）
        return true;
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
        System.out.println("DEBUG: Tick at " + pos + " - isSource: " + fluidState.isSource() +
                         ", amount: " + fluidState.getAmount());

        if (!fluidState.isSource()) {
            FluidState newFluidState = this.getNewLiquid(level, pos, level.getBlockState(pos));
            int spreadDelay = this.getSpreadDelay(level, pos, fluidState, newFluidState);

            System.out.println("DEBUG: Old state: " + fluidState.getAmount() +
                             ", New state: " + (newFluidState.isEmpty() ? "EMPTY" : String.valueOf(newFluidState.getAmount())));

            if (newFluidState.isEmpty()) {
                // 流体消散时，通知邻居重新检查
                System.out.println("DEBUG: Fluid dissipating at " + pos + ", notifying neighbors");
                this.notifyNeighborsOfChange(level, pos);
                fluidState = newFluidState;
                blockState = Blocks.AIR.defaultBlockState();
                level.setBlock(pos, blockState, 3);
            } else if (newFluidState != fluidState) {
                System.out.println("DEBUG: Fluid state changed at " + pos);
                fluidState = newFluidState;
                blockState = newFluidState.createLegacyBlock();
                level.setBlock(pos, blockState, 3);
                level.scheduleTick(pos, newFluidState.getType(), spreadDelay);
            } else {
                // 即使状态没有改变，也要定期重新调度tick以确保消散检查
                System.out.println("DEBUG: Rescheduling tick at " + pos + " for continued checking");
                level.scheduleTick(pos, fluidState.getType(), this.getTickDelay(level) * 2);
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
                    // 确保不创建源方块
                    int safeNewStrength = Math.min(newStrength, 7);
                    FluidState newFluidState = this.getFlowing(safeNewStrength, false);

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
}
