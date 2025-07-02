# 严格向上流动逻辑

## 问题描述

在之前的实现中，魔法水在向上流动过程中会意外地向四周扩散，即使上方没有被阻挡。这导致：
- 流体在向上流动的同时向侧面扩散
- 无法形成纯粹的向上流动柱
- 流体行为不符合"反重力"的预期

## 根本原因

### 1. 条件判断不够严格
```java
// 原始逻辑：条件过于宽松
if (this.canFlowUpward(level, pos, blockState, fluidState)) {
    // 向上流动
} else {
    // 向侧面扩散 - 这里的条件判断不够严格
}
```

### 2. 流体强度计算问题
- 流体强度递减过快，导致无法持续向上流动
- 强度阈值设置不当

### 3. 重复检查缺失
- 在决定向侧面扩散时没有再次确认是否真的无法向上流动

## 解决方案

### 1. 更严格的向上流动检查
```java
private boolean canFlowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    // 只有当流体有足够强度时才能向上流动
    if (fluidState.getAmount() <= 1) {
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
    
    return this.canMaybePassThrough(level, pos, blockState, Direction.UP, abovePos, aboveState, aboveFluidState) &&
           this.canHoldSpecificFluid(level, abovePos, aboveState, fluidState.getType());
}
```

**关键改进**:
- 强度阈值检查：`fluidState.getAmount() <= 1`
- 严格的上方状态检查
- 相同类型流体的强度比较

### 2. 双重确认的侧面扩散
```java
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
    
    // ... 侧面扩散逻辑
}
```

**关键改进**:
- 双重检查：再次调用`canFlowUpward`
- 明确的空间检查：确认上方真的被阻挡
- 早期返回：避免不必要的侧面扩散

### 3. 更严格的流体强度管理
```java
private void flowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    // 计算向上流动的强度，确保有足够的推力
    int currentStrength = fluidState.getAmount();
    int upwardStrength = Math.max(1, currentStrength - this.getDropOff(level));
    
    // 如果强度太低，不进行向上流动
    if (upwardStrength <= 0) {
        return;
    }
    
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
    
    // ... 执行向上流动
}
```

## 流动逻辑对比

### 修复前的行为
```
时刻1: [S] 源方块放置
时刻2: [M] 向上流动 + [M][M][M] 同时向侧面扩散
时刻3: [M] 继续向上 + 更多侧面扩散
```

### 修复后的行为
```
时刻1: [S] 源方块放置
时刻2: [M] 只向上流动
时刻3: [M] 继续向上流动
时刻4: [#] 遇到障碍物
时刻5: [M][M][M] 此时才向侧面扩散
```

## 关键检查点

### 1. 强度阈值
```java
// 确保有足够强度向上流动
if (fluidState.getAmount() <= 1) {
    return false;
}
```

### 2. 上方空间检查
```java
// 确保上方真的有空间
if (aboveState.isAir() || aboveState.canBeReplaced()) {
    // 可以向上流动
}
```

### 3. 流体强度比较
```java
// 只有当上方流体强度更低时才替换
if (aboveFluidState.getType().isSame(this)) {
    return aboveFluidState.getAmount() < fluidState.getAmount();
}
```

### 4. 双重确认
```java
// 在侧面扩散前再次检查
if (this.canFlowUpward(level, pos, blockState, fluidState)) {
    return; // 还能向上，不要侧面扩散
}
```

## 测试场景

### 场景1: 开阔空间（应该只向上）
```
预期行为:
[ ]     [ ]
[ ] →   [M]  ← 只向上流动
[S]     [S]

不应该出现:
[M][M][M]
[ ][M][ ]  ← 不应该有侧面扩散
[ ][S][ ]
```

### 场景2: 遇到障碍（此时才侧面扩散）
```
预期行为:
[#]     [#]
[ ] →   [M][M][M]  ← 此时才侧面扩散
[S]     [ ][S][ ]
```

### 场景3: 部分阻挡（绕过障碍继续向上）
```
预期行为:
[ ][#][ ]     [M][#][M]  ← 绕过障碍向上
[ ][ ][ ] →   [ ][M][ ]
[ ][S][ ]     [ ][S][ ]
```

## 调试技巧

### 添加调试日志
```java
private boolean canFlowUpward(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    if (fluidState.getAmount() <= 1) {
        System.out.println("Cannot flow upward: insufficient strength at " + pos);
        return false;
    }
    
    BlockPos abovePos = pos.above();
    BlockState aboveState = level.getBlockState(abovePos);
    
    if (!aboveState.isAir() && !aboveState.canBeReplaced()) {
        System.out.println("Cannot flow upward: blocked by " + aboveState + " at " + abovePos);
        return false;
    }
    
    System.out.println("Can flow upward from " + pos + " to " + abovePos);
    return true;
}
```

### 监控侧面扩散触发
```java
private void spreadToSidesOnly(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState blockState) {
    if (this.canFlowUpward(level, pos, blockState, fluidState)) {
        System.out.println("Prevented side spread: can still flow upward at " + pos);
        return;
    }
    
    System.out.println("Starting side spread: truly blocked at " + pos);
    // ... 侧面扩散逻辑
}
```

## 性能考虑

### 优化点
1. **早期返回**: 多个检查点都有早期返回，减少不必要的计算
2. **双重检查开销**: 虽然有额外的检查，但避免了错误的侧面扩散
3. **明确条件**: 减少模糊的判断逻辑

### 潜在问题
1. **检查频率**: 每次都进行双重检查可能增加计算量
2. **阈值调整**: 强度阈值可能需要根据实际效果调整

## 总结

通过实施严格的向上流动逻辑，我们实现了：

1. ✅ **纯粹向上流动**: 在开阔空间中只向上流动
2. ✅ **条件侧面扩散**: 只有在真正被阻挡时才向侧面扩散
3. ✅ **双重确认**: 防止意外的侧面扩散
4. ✅ **强度管理**: 确保有足够强度支持向上流动
5. ✅ **明确条件**: 每个判断都有明确的逻辑

这样的实现确保魔法水真正表现出"反重力"的特性，只有在物理上无法继续向上流动时才会寻找其他出路！
