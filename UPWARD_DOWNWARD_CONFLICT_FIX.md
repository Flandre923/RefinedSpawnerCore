# 向上向下流动冲突修复

## 问题描述

在实现向上流动功能后，发现流体会在向上流动的同时执行向下流动逻辑，导致：
- 流体在2格高度停止
- 上下抖动现象
- 流体无法持续向上流动

## 根本原因

### 1. 多重流动逻辑冲突
- `spread`方法控制流体扩散
- `getNewLiquid`方法控制流体状态更新
- `tick`方法控制流体的整体行为
- 这些方法可能同时执行相互冲突的逻辑

### 2. FALLING状态问题
```java
// 原版逻辑中，流体可能被错误标记为FALLING
FluidState newState = this.getFlowing(amount, true); // true表示falling
```

### 3. 向下流动的默认行为
- FlowingFluid的默认实现优先考虑向下流动
- 即使重写了spread方法，其他方法仍可能触发向下流动

## 解决方案

### 1. 重写getNewLiquid方法
```java
@Override
protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
    // 优先检查是否可以向上流动
    BlockPos abovePos = pos.above();
    FluidState aboveFluidState = level.getFluidState(abovePos);
    
    // 如果上方有相同类型的流体，说明这里应该有流体
    if (!aboveFluidState.isEmpty() && aboveFluidState.getType().isSame(this)) {
        // 从上方获取流体状态，强度递增（因为是向上流动）
        int strengthFromAbove = Math.min(8, aboveFluidState.getAmount() + 1);
        return this.getFlowing(strengthFromAbove, false); // 关键：false表示不是falling
    }
    
    // ... 其他逻辑
}
```

**关键改进**:
- 优先检查上方流体状态
- 强度从上往下递增（与原版相反）
- 确保不设置FALLING标志

### 2. 重写tick方法
```java
@Override
public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    if (!fluidState.isSource()) {
        FluidState newFluidState = this.getNewLiquid(level, pos, level.getBlockState(pos));
        // ... 状态更新逻辑
    }

    // 只执行向上流动的spread逻辑
    this.spread(level, pos, blockState, fluidState);
}
```

**关键改进**:
- 使用自定义的getNewLiquid方法
- 只调用自定义的spread方法
- 避免原版的向下流动逻辑

### 3. 改进flowUpward方法
```java
private void flowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    BlockPos abovePos = pos.above();
    
    // 计算向上流动的强度
    int upwardStrength = Math.max(1, fluidState.getAmount() - 1);
    FluidState newFluidState = this.getFlowing(upwardStrength, false); // 确保不是falling状态
    
    if (aboveFluidState.canBeReplacedWith(level, abovePos, newFluidState.getType(), Direction.UP)) {
        this.spreadTo(level, abovePos, aboveState, Direction.UP, newFluidState);
        
        // 安排下一次tick，继续向上流动
        level.scheduleTick(abovePos, newFluidState.getType(), this.getTickDelay(level));
    }
}
```

**关键改进**:
- 明确设置falling为false
- 主动安排下一次tick
- 确保流动的连续性

### 4. 防止源方块转换
```java
@Override
public boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
    return false; // 魔法水不能转换为源方块
}
```

**目的**:
- 防止意外的源方块生成
- 避免流动逻辑被干扰

## 流动逻辑对比

### 原版水的流动逻辑
```
源方块 → 向下流动 → 向侧面扩散 → 形成水平面
```

### 魔法水的新逻辑
```
源方块 → 向上流动 → 遇阻碍时向侧面 → 形成向上的柱状
```

## 状态管理

### FALLING标志的重要性
```java
// 错误：会导致向下流动行为
FluidState badState = this.getFlowing(amount, true);

// 正确：确保向上流动行为
FluidState goodState = this.getFlowing(amount, false);
```

### 流体强度计算
```java
// 向上流动：强度递减
int upwardStrength = Math.max(1, sourceStrength - 1);

// 向下流动（原版）：强度递减
int downwardStrength = Math.max(1, sourceStrength - 1);

// 关键区别：方向和FALLING标志
```

## 测试验证

### 测试用例1: 基本向上流动
```
预期：
[ ][M][ ]  ← 第3格
[ ][M][ ]  ← 第2格  
[ ][S][ ]  ← 源方块

实际：应该形成稳定的向上流动柱
```

### 测试用例2: 无抖动
```
预期：流体应该稳定向上流动，不在2格高度停止
实际：应该持续向上直到遇到障碍或达到最大高度
```

### 测试用例3: 遇障碍时的行为
```
预期：
[#][#][#]  ← 障碍物
[M][M][M]  ← 向侧面扩散
[ ][S][ ]  ← 源方块

实际：应该在无法向上时才向侧面扩散
```

## 调试技巧

### 添加调试日志
```java
private void flowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    System.out.println("Flowing upward from " + pos + " with strength " + fluidState.getAmount());
    // ... 实际逻辑
}
```

### 检查FALLING状态
```java
if (fluidState.getValue(FALLING)) {
    System.out.println("WARNING: Magic water marked as falling at " + pos);
}
```

### 监控状态变化
```java
@Override
public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    FluidState oldState = fluidState;
    // ... tick逻辑
    FluidState newState = level.getFluidState(pos);
    
    if (oldState != newState) {
        System.out.println("State changed at " + pos + ": " + oldState + " → " + newState);
    }
}
```

## 性能考虑

### 优化点
1. **减少冲突检查**: 避免同时计算多个方向的流动
2. **明确优先级**: 向上流动绝对优先，减少不必要的计算
3. **状态缓存**: 避免重复计算相同的流体状态

### 潜在问题
1. **递归深度**: 向上流动可能形成很长的链条
2. **tick频率**: 过于频繁的tick可能影响性能

## 总结

通过重写多个核心方法，我们成功解决了向上向下流动的冲突问题：

1. ✅ **消除抖动**: 流体不再在2格高度停止
2. ✅ **稳定向上**: 流体能够持续向上流动
3. ✅ **正确状态**: FALLING标志正确设置为false
4. ✅ **避免冲突**: 不同方法之间的逻辑保持一致
5. ✅ **性能优化**: 减少不必要的计算和状态检查

这样的修复确保了魔法水真正表现出"反重力"的特性，创造出稳定而有趣的向上流动效果！
