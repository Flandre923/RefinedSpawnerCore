# 向上流动流体实现原理

## 概述
魔法水的向上流动功能是通过重写FlowingFluid的核心方法和调整FluidType属性来实现的。

## 实现方法

### 1. 负密度设置
```java
.density(-1000) // 负密度，使流体向上流动
.viscosity(500) // 降低粘度，使流动更快
```
- **负密度**: 告诉游戏这个流体比空气轻，应该向上流动
- **低粘度**: 使流体流动更快，增强向上流动效果

### 2. 重写spread方法
```java
@Override
protected void spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    // 检查是否可以向上流动
    if (this.canFlowUpward(level, pos, blockState, fluidState)) {
        // 向上流动
        this.flowUpward(level, pos, blockState, fluidState);
        return; // 向上流动成功，不进行侧面扩散
    }

    // 只有在无法向上流动时才考虑侧面扩散
    this.spreadToSidesOnly(level, pos, fluidState, blockState);
}
```

**核心逻辑**:
1. **绝对优先向上**: 流体只要能向上流动就绝不向侧面流动
2. **条件侧面流动**: 只有在完全无法向上流动时才向水平方向流动
3. **忽略下方影响**: 下方的方块状态不影响侧面扩散决策
4. **避免向下**: 完全避免向下流动的逻辑

### 3. 自定义流体状态计算
```java
protected FluidState getNewLiquidForUpwardFlow(ServerLevel level, BlockPos pos, BlockState state) {
    // 检查下方的流体作为流动源
    BlockPos belowPos = pos.below();
    FluidState belowFluidState = belowState.getFluidState();
    if (belowFluidState.getType().isSame(this)) {
        // 从下方获取流体，向上传递
        return this.getFlowing(Math.max(1, belowFluidState.getAmount() - this.getDropOff(level)), false);
    }
}
```

**关键特性**:
- 流体从下方获取"能量"向上流动
- 流动强度随高度递减
- 保持流体的连续性

### 4. 辅助方法实现

#### 碰撞检测
```java
private boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos pos, BlockState state, BlockPos spreadPos, BlockState spreadState) {
    // 检查流体是否可以通过方块边界
    return !Shapes.mergedFaceOccludes(voxelshape1, voxelshape, direction);
}
```

#### 容器检测
```java
private boolean canHoldAnyFluid(BlockState state) {
    // 检查方块是否可以容纳流体
    return !state.blocksMotion() && !state.is(Blocks.LADDER);
}
```

#### 流体洞检测
```java
private boolean isFluidHole(BlockGetter level, BlockPos pos, BlockState state, BlockPos belowPos, BlockState belowState) {
    // 自定义实现，替代不可访问的isWaterHole方法
    if (!this.canPassThroughWall(Direction.DOWN, level, pos, state, belowPos, belowState)) {
        return false;
    } else {
        return belowState.getFluidState().getType().isSame(this) ? true : this.canHoldFluid(level, belowPos, belowState, this.getFlowing());
    }
}
```

## 流动行为

### 向上流动优先级
1. **绝对优先**: 向上流动到空气或可替换方块
2. **条件触发**: 只有在无法向上流动时才向水平方向流动
3. **完全避免**: 永不向下流动

### 流动条件
- 上方方块必须是空气或可替换的
- 流体必须有足够的"压力"(从下方获得)
- 不能被固体方块阻挡

### 流动限制
- 流体强度随高度递减
- 达到最大高度后停止流动
- 遇到不可穿透的方块时停止

## 视觉效果

### 密度影响
- 负密度使实体在流体中有不同的物理表现
- 可能影响粒子效果的方向
- 影响流体的渲染优先级

### 流动动画
- 流体纹理仍然使用标准的流动动画
- 但流动方向是向上的
- 创造出独特的视觉效果

## 技术细节

### 性能考虑
- 向上流动可能比向下流动消耗更多性能
- 需要更多的碰撞检测
- 流体更新频率可能需要调整

### 兼容性
- 与原版流体系统兼容
- 不影响其他流体的行为
- 可以与原版水共存

### 限制
- 流体仍然受到区块边界限制
- 不能无限向上流动
- 受到游戏物理引擎的约束

## 扩展可能性

### 进一步优化
1. **粒子效果**: 添加向上流动的粒子
2. **声音效果**: 自定义向上流动的声音
3. **交互效果**: 与其他方块的特殊交互
4. **高度限制**: 设置最大流动高度

### 其他应用
- 可以应用于其他自定义流体
- 创建不同密度的流体混合效果
- 实现更复杂的流体物理

## 调试提示

### 常见问题
1. **流体不向上流动**: 检查密度是否为负值
2. **流动太慢**: 调整粘度值
3. **流体消失**: 检查流体状态计算逻辑
4. **性能问题**: 优化spread方法的调用频率

### 测试建议
1. 在平坦地面测试基本向上流动
2. 测试与不同方块类型的交互
3. 测试流体的边界行为
4. 测试多个流体源的交互
