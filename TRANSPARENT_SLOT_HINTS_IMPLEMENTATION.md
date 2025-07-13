# 半透明槽位提示实现

## 问题描述

原始的槽位提示渲染存在以下问题：
1. **无透明效果**：提示物品与正常物品透明度相同，无法区分
2. **视觉混淆**：用户难以区分提示和实际物品
3. **缺乏视觉反馈**：静态显示，缺乏动态效果

## 解决方案

### 🎨 半透明渲染系统

#### 核心改进
1. **真正的透明度支持**：使用RenderSystem设置全局透明度
2. **多种视觉效果**：脉动、边框、背景等不同效果
3. **分类渲染**：不同类型槽位使用不同的视觉样式

#### 实现方法

##### 1. 基础半透明渲染
```java
public static void renderTransparentItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, float alpha) {
    // 启用混合模式
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    
    // 设置全局透明度
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
    
    // 渲染物品
    guiGraphics.renderItem(itemStack, x, y);
    
    // 恢复设置
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    RenderSystem.disableBlend();
}
```

##### 2. 脉动效果渲染
```java
public static void renderSlotHint(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
    long gameTime = System.currentTimeMillis();
    
    // 计算脉动透明度 (0.2f 到 0.5f 之间脉动)
    float pulseAlpha = 0.2f + 0.3f * (float)(Math.sin(gameTime * 0.003) * 0.5 + 0.5);
    
    // 渲染半透明背景
    guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0x40FFFFFF);
    
    // 渲染半透明物品
    renderTransparentItem(guiGraphics, itemStack, x, y, pulseAlpha);
}
```

##### 3. 边框效果渲染
```java
public static void renderSlotHintWithBorder(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, int borderColor) {
    // 渲染边框
    guiGraphics.fill(x - 1, y - 1, x + 17, y, borderColor); // 上边框
    guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, borderColor); // 下边框
    guiGraphics.fill(x - 1, y, x, y + 16, borderColor); // 左边框
    guiGraphics.fill(x + 16, y, x + 17, y + 16, borderColor); // 右边框
    
    // 渲染半透明背景
    guiGraphics.fill(x, y, x + 16, y + 16, 0x20000000);
    
    // 渲染半透明物品
    renderTransparentItem(guiGraphics, itemStack, x, y, 0.6f);
}
```

### 🎯 分类视觉效果

#### 槽位类型与效果映射

| 槽位类型 | 视觉效果 | 颜色 | 透明度 | 说明 |
|----------|----------|------|--------|------|
| **普通模块** | 脉动效果 | 白色背景 | 0.2-0.5f | 动态脉动，吸引注意 |
| **模拟升级** | 金色边框 | 金色边框 | 0.6f | 重要功能，金色突出 |
| **抢夺升级** | 紫色边框 | 紫色边框 | 0.6f | 高级功能，紫色标识 |
| **斩首升级** | 紫色边框 | 紫色边框 | 0.6f | 高级功能，紫色标识 |

#### 渲染逻辑
```java
if (slotTypes[i] == SpawnerModuleType.SIMULATION_UPGRADE) {
    // 模拟升级槽位使用特殊的金色边框
    TransparentItemRenderer.renderSlotHintWithBorder(
        guiGraphics, hintItem, slotX, slotY, 0x80FFD700 // 半透明金色
    );
} else if (slotTypes[i] == SpawnerModuleType.LOOTING_UPGRADE || 
           slotTypes[i] == SpawnerModuleType.BEHEADING_UPGRADE) {
    // 升级模块使用紫色边框
    TransparentItemRenderer.renderSlotHintWithBorder(
        guiGraphics, hintItem, slotX, slotY, 0x80800080 // 半透明紫色
    );
} else {
    // 普通模块使用脉动效果
    TransparentItemRenderer.renderSlotHint(
        guiGraphics, hintItem, slotX, slotY
    );
}
```

### 🔧 技术实现

#### 关键技术点

1. **混合模式管理**：
   - `RenderSystem.enableBlend()` - 启用透明度混合
   - `RenderSystem.defaultBlendFunc()` - 使用默认混合函数
   - `RenderSystem.disableBlend()` - 渲染后禁用混合

2. **全局颜色设置**：
   - `RenderSystem.setShaderColor(r, g, b, alpha)` - 设置渲染颜色和透明度
   - 渲染后必须恢复为 `(1.0f, 1.0f, 1.0f, 1.0f)`

3. **时间基础动画**：
   - 使用 `System.currentTimeMillis()` 获取当前时间
   - 通过正弦函数计算脉动效果
   - 控制动画速度和幅度

#### 性能优化

1. **渲染状态管理**：
   - 使用 `poseStack.pushPose()` 和 `popPose()` 保护渲染状态
   - 确保渲染后恢复原始设置

2. **条件渲染**：
   - 只在槽位为空且有对应提示时渲染
   - 根据槽位类型选择合适的渲染方法

3. **批量处理**：
   - 在同一个渲染循环中处理所有槽位
   - 避免频繁的状态切换

### 🎨 视觉效果对比

#### 改进前
- ❌ 提示物品与正常物品无法区分
- ❌ 静态显示，缺乏动态效果
- ❌ 无视觉层次，用户体验差

#### 改进后
- ✅ 明显的半透明效果，易于区分
- ✅ 动态脉动效果，吸引用户注意
- ✅ 分类颜色标识，功能层次清晰
- ✅ 边框和背景效果，视觉反馈丰富

### 🔮 扩展功能

#### 可配置的透明度
```java
// 可以根据用户设置调整透明度
public static float HINT_ALPHA = 0.4f; // 可配置的默认透明度
public static float PULSE_MIN_ALPHA = 0.2f; // 脉动最小透明度
public static float PULSE_MAX_ALPHA = 0.5f; // 脉动最大透明度
```

#### 更多视觉效果
```java
// 发光效果
public static void renderGlowingHint(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
    // 渲染发光层
    for (int i = 0; i < 3; i++) {
        renderTransparentItem(guiGraphics, itemStack, x - i, y, 0.1f);
        renderTransparentItem(guiGraphics, itemStack, x + i, y, 0.1f);
        renderTransparentItem(guiGraphics, itemStack, x, y - i, 0.1f);
        renderTransparentItem(guiGraphics, itemStack, x, y + i, 0.1f);
    }
    // 渲染主体
    renderTransparentItem(guiGraphics, itemStack, x, y, 0.6f);
}
```

#### 动画效果
```java
// 旋转动画
public static void renderRotatingHint(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
    var poseStack = guiGraphics.pose();
    poseStack.pushPose();
    
    // 移动到物品中心
    poseStack.translate(x + 8, y + 8, 0);
    
    // 旋转
    float rotation = (System.currentTimeMillis() * 0.01f) % 360;
    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
    
    // 移回原位
    poseStack.translate(-8, -8, 0);
    
    renderTransparentItem(guiGraphics, itemStack, 0, 0, 0.5f);
    
    poseStack.popPose();
}
```

### 📝 使用说明

#### 基本用法
```java
// 简单半透明提示
TransparentItemRenderer.renderSimpleTransparentItem(guiGraphics, itemStack, x, y);

// 脉动效果提示
TransparentItemRenderer.renderSlotHint(guiGraphics, itemStack, x, y);

// 带边框的提示
TransparentItemRenderer.renderSlotHintWithBorder(guiGraphics, itemStack, x, y, 0x80FFD700);
```

#### 自定义透明度
```java
// 自定义透明度
TransparentItemRenderer.renderTransparentItem(guiGraphics, itemStack, x, y, 0.3f);

// 脉动透明度
TransparentItemRenderer.renderPulsingTransparentItem(guiGraphics, itemStack, x, y, gameTime);
```

### ✅ 测试验证

#### 测试场景
1. **空槽位显示**：确认空槽位显示半透明提示
2. **有物品槽位**：确认有物品时不显示提示
3. **不同槽位类型**：验证不同类型使用不同效果
4. **动画效果**：确认脉动动画正常工作
5. **性能测试**：确认渲染性能无明显影响

#### 预期效果
- ✅ 半透明提示清晰可见但不干扰正常物品
- ✅ 脉动效果平滑自然
- ✅ 边框颜色正确显示
- ✅ 不同槽位类型有明显的视觉区别
- ✅ 渲染性能良好

现在的槽位提示具有真正的半透明效果，提供了丰富的视觉反馈！
