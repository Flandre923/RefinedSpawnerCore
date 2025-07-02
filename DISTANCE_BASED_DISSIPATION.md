# 基于距离的消散机制

## 问题诊断

根据你提供的调试日志，我发现了问题的根源：

### 关键发现
```
DEBUG: Found source at BlockPos{x=191, y=44, z=-501} from BlockPos{x=191, y=45, z=-501}
DEBUG: Tick at BlockPos{x=191, y=36, z=-501} - isSource: true, amount: 8
```

**问题**：
- 真正的源头在 `y=36`
- 但算法在 `y=44` 找到了"假源头"
- 这导致所有流体认为自己仍然连接到源头

### 根本原因
1. **错误的源方块识别** - 某个流动流体被错误标记为源方块
2. **递归搜索缺陷** - 复杂的递归逻辑容易出错
3. **状态不一致** - 流体状态和实际方块状态不匹配

## 新的解决方案

### 1. 距离检查替代递归搜索
```java
// 旧方法：复杂的递归搜索
private boolean hasSourceConnection(ServerLevel level, BlockPos pos, Set<BlockPos> visited) {
    // 复杂的递归逻辑，容易出错
}

// 新方法：简单的距离检查
private boolean isWithinSourceRange(ServerLevel level, BlockPos pos, int maxRange) {
    // 在指定范围内搜索真正的源方块
    for (int x = -maxRange; x <= maxRange; x++) {
        for (int y = -maxRange; y <= maxRange; y++) {
            for (int z = -maxRange; z <= maxRange; z++) {
                BlockPos checkPos = pos.offset(x, y, z);
                FluidState checkFluidState = level.getFluidState(checkPos);
                
                // 严格检查：必须是真正的源方块
                if (checkFluidState.getType().isSame(this) && 
                    checkFluidState.isSource() && 
                    checkFluidState.getAmount() == 8) {
                    
                    if (this.isReachableFrom(level, pos, checkPos, maxRange)) {
                        return true;
                    }
                }
            }
        }
    }
    return false;
}
```

### 2. 严格的源方块验证
```java
// 严格检查源方块的条件
if (checkFluidState.getType().isSame(this) && 
    checkFluidState.isSource() && 
    checkFluidState.getAmount() == 8) {
    // 只有满足所有条件才认为是真正的源方块
}
```

### 3. 路径可达性检查
```java
// 检查从流体到源头是否有连续的流体路径
private boolean hasFluidPath(ServerLevel level, BlockPos from, BlockPos to) {
    // 对于垂直路径（向上流动），检查每一格都有流体
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
    return true; // 非垂直路径暂时允许
}
```

## 算法优势

### 1. 简单可靠
- 不依赖复杂的递归逻辑
- 直接搜索真正的源方块
- 减少状态不一致的可能性

### 2. 性能可控
- 搜索范围可限制（16格）
- 避免无限递归
- 早期返回优化

### 3. 调试友好
- 清晰的搜索过程
- 明确的判断条件
- 详细的日志输出

## 预期效果

### 修复前的问题
```
真实源头: y=36 (amount=8, isSource=true)
假源头: y=44 (amount=?, isSource=true) ← 错误！
结果: 所有流体认为仍有源头连接
```

### 修复后的行为
```
搜索范围: y=36附近16格
找到源头: y=36 (amount=8, isSource=true) ✓
路径检查: y=36到y=51连续流体 ✓
结果: 流体正确连接到源头

移除源头后:
搜索范围: y=36附近16格
找到源头: 无 ✗
结果: 所有流体消散
```

## 调试信息解读

### 新的调试输出
```
DEBUG: Found reachable source at [x,y,z] from [x,y,z]
DEBUG: No reachable source found within range 16 from [x,y,z]
DEBUG: Verifying source at [x,y,z] - isSource: true, amount: 8, result: true
```

### 问题诊断
1. **如果看到多个"Found reachable source"** - 可能有多个源头
2. **如果"amount"不是8** - 源方块状态异常
3. **如果"No reachable source found"但流体仍存在** - 算法工作正常，流体应该消散

## 性能考虑

### 搜索范围优化
```java
// 16格搜索范围：16³ = 4096个位置
// 对于大多数情况足够，且性能可接受
private boolean isWithinSourceRange(ServerLevel level, BlockPos pos, int maxRange) {
    // maxRange = 16，平衡性能和功能
}
```

### 早期返回
```java
// 找到第一个有效源头就返回
if (this.isReachableFrom(level, pos, checkPos, maxRange)) {
    return true; // 立即返回，不继续搜索
}
```

### 路径检查优化
```java
// 优先检查垂直路径（向上流动的主要情况）
if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
    // 快速垂直路径检查
}
```

## 测试验证

### 测试1: 基本消散
```
1. 放置源头在y=36
2. 等待流体流动到y=51
3. 移除源头
4. 观察调试输出：应该看到"No reachable source found"
5. 验证：所有流体应该消散
```

### 测试2: 距离限制
```
1. 创建超过16格的流体链
2. 观察远端流体是否自动消散
3. 验证：距离检查是否正常工作
```

### 测试3: 路径中断
```
1. 创建流体链
2. 在中间移除一格流体
3. 观察上方流体是否消散
4. 验证：路径检查是否正常工作
```

## 回退方案

如果距离检查仍有问题，可以使用更简单的方案：

### 方案A: 时间限制
```java
// 给每个流体添加生存时间
private long creationTime = System.currentTimeMillis();

if (System.currentTimeMillis() - creationTime > 30000) { // 30秒后自动消散
    return Fluids.EMPTY.defaultFluidState();
}
```

### 方案B: 强度限制
```java
// 距离源头太远的流体自动消散
if (fluidState.getAmount() <= 1) {
    return Fluids.EMPTY.defaultFluidState();
}
```

## 总结

新的距离检查方法应该能解决你遇到的问题：

1. ✅ **消除假源头** - 严格验证源方块状态
2. ✅ **简化逻辑** - 用距离检查替代复杂递归
3. ✅ **提高可靠性** - 减少状态不一致的可能性
4. ✅ **保持性能** - 限制搜索范围和优化算法
5. ✅ **便于调试** - 清晰的日志和判断逻辑

请测试这个新版本，特别关注：
- 是否还能找到y=44的"假源头"
- 移除真正源头后是否能正确消散
- 调试输出是否显示正确的源头位置
