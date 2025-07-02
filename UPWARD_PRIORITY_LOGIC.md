# 向上流动优先逻辑详解

## 问题描述

原始实现中，魔法水虽然设置了负密度，但仍然会按照原版逻辑优先向四周扩散，而不是专注于向上流动。这导致流体行为不符合预期。

## 解决方案

### 核心思想
**绝对优先向上流动** - 流体只要能向上流动就绝不向侧面流动，只有在完全无法向上流动时才考虑侧面扩散。

### 实现逻辑

#### 1. 主要流动控制
```java
@Override
protected void spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    if (!fluidState.isEmpty()) {
        // 检查是否可以向上流动
        if (this.canFlowUpward(level, pos, blockState, fluidState)) {
            // 向上流动
            this.flowUpward(level, pos, blockState, fluidState);
            return; // 关键：向上流动成功后立即返回，不执行侧面扩散
        }
        
        // 只有在无法向上流动时才考虑侧面扩散
        this.spreadToSidesOnly(level, pos, fluidState, blockState);
    }
}
```

#### 2. 向上流动检测
```java
private boolean canFlowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    BlockPos abovePos = pos.above();
    BlockState aboveState = level.getBlockState(abovePos);
    FluidState aboveFluidState = aboveState.getFluidState();
    
    // 检查上方是否可以容纳流体
    return this.canMaybePassThrough(level, pos, blockState, Direction.UP, abovePos, aboveState, aboveFluidState) &&
           this.canHoldSpecificFluid(level, abovePos, aboveState, fluidState.getType());
}
```

#### 3. 执行向上流动
```java
private void flowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    BlockPos abovePos = pos.above();
    BlockState aboveState = level.getBlockState(abovePos);
    FluidState aboveFluidState = aboveState.getFluidState();
    
    FluidState newFluidState = this.getNewLiquidForUpwardFlow(level, abovePos, aboveState);
    Fluid fluid = newFluidState.getType();
    
    if (aboveFluidState.canBeReplacedWith(level, abovePos, fluid, Direction.UP)) {
        this.spreadTo(level, abovePos, aboveState, Direction.UP, newFluidState);
    }
}
```

#### 4. 条件侧面扩散
```java
private void spreadToSidesOnly(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState blockState) {
    int flowStrength = fluidState.getAmount() - this.getDropOff(level);
    
    // 如果是下降流体，保持较高的流动强度
    if (fluidState.getValue(FALLING)) {
        flowStrength = Math.max(flowStrength, 6);
    }

    if (flowStrength > 0) {
        // 向四个水平方向扩散
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            // ... 侧面扩散逻辑
        }
    }
}
```

## 关键改进

### 1. 绝对优先级
- **原版逻辑**: 同时考虑向上和向侧面流动，可能优先侧面
- **新逻辑**: 只要能向上流动就绝不向侧面流动

### 2. 条件触发
- **原版逻辑**: 基于复杂的条件决定流动方向
- **新逻辑**: 简单明确 - 能上则上，不能上才侧面

### 3. 独立判断
- **原版逻辑**: 下方方块状态影响侧面扩散
- **新逻辑**: 侧面扩散不受下方方块影响

### 4. 流动强度管理
```java
// 向上流动时保持较强的流动力
int upwardStrength = Math.max(1, belowFluidState.getAmount() - 1);

// 向侧面流动时强度递减
int newStrength = Math.max(1, flowStrength - 2);
```

## 流动行为对比

### 原版水的行为
1. 放置水源
2. 同时向下和向侧面流动
3. 优先填充低处
4. 形成水平面

### 魔法水的新行为
1. 放置魔法水源
2. **优先向上流动**
3. 只有遇到障碍物才向侧面流动
4. 形成向上的"水柱"

## 实际效果

### 场景1: 开阔空间
```
放置前:     放置后:
[ ][ ][ ]   [ ][M][ ]  ← 魔法水向上流动
[ ][ ][ ]   [ ][M][ ]  ← 继续向上
[ ][P][ ]   [ ][M][ ]  ← 源方块位置
```

### 场景2: 遇到障碍
```
放置前:     放置后:
[#][#][#]   [#][#][#]  ← 固体方块阻挡
[ ][ ][ ]   [M][M][M]  ← 无法向上，向侧面扩散
[ ][P][ ]   [ ][M][ ]  ← 源方块位置
```

### 场景3: 部分阻挡
```
放置前:     放置后:
[ ][#][ ]   [ ][#][ ]  ← 中间有障碍
[ ][ ][ ]   [M][ ][M]  ← 绕过障碍向上
[ ][P][ ]   [ ][M][ ]  ← 源方块位置
```

## 性能考虑

### 优化点
1. **早期返回**: 向上流动成功后立即返回，减少不必要的计算
2. **简化判断**: 减少复杂的条件检查
3. **明确优先级**: 避免同时计算多个方向

### 潜在问题
1. **频繁向上检查**: 每次tick都检查向上流动可能性
2. **递归深度**: 向上流动可能形成很长的链条

### 解决方案
```java
// 可以添加高度限制
private static final int MAX_UPWARD_FLOW_HEIGHT = 64;

// 或者添加流动延迟
public int getTickDelay(LevelReader levelReader) {
    return 3; // 稍微增加延迟以减少计算频率
}
```

## 调试和测试

### 测试用例
1. **基础向上流动**: 在平地放置，观察是否向上流动
2. **障碍物测试**: 在上方放置方块，观察是否向侧面扩散
3. **高度限制**: 测试流体能流动到多高
4. **性能测试**: 大量流体的性能表现

### 调试技巧
```java
// 添加调试日志
private void flowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    System.out.println("Magic water flowing upward from " + pos + " to " + pos.above());
    // ... 实际流动逻辑
}
```

## 总结

通过重新设计流动逻辑，我们实现了真正的"向上流动优先"行为：

1. ✅ **绝对优先向上**: 能上则上，绝不侧面
2. ✅ **条件侧面扩散**: 只有无法向上时才侧面流动
3. ✅ **忽略下方影响**: 下方状态不影响侧面扩散
4. ✅ **保持流动性**: 仍然能够正常扩散和流动
5. ✅ **性能友好**: 简化逻辑，提高效率

这样的设计让魔法水真正表现出"反重力"的特性，创造出独特而有趣的游戏体验！
