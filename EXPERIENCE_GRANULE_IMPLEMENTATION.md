# 经验颗粒物品实现文档

## 概述

经验颗粒是一个新的物品，当机器使用了模拟升级击杀生物时会掉落。玩家可以右键使用经验颗粒来在当前位置生成经验球，按住Shift可以批量使用整个堆叠。

## 实现组件

### 1. ExperienceGranuleItem.java
- **位置**: `src/main/java/com/example/examplemod/item/ExperienceGranuleItem.java`
- **功能**:
  - 继承自Item类
  - 实现右键使用功能
  - 支持Shift批量使用
  - 存储经验值在NBT数据中
  - 显示工具提示信息
  - 具有附魔光效

### 2. ExperienceGranuleDropEvent.java
- **位置**: `src/main/java/com/example/examplemod/event/ExperienceGranuleDropEvent.java`
- **功能**:
  - 监听LivingDropsEvent事件
  - 检测SpawnerFakePlayer击杀
  - 计算基础经验值
  - 应用抢夺附魔影响
  - 生成经验颗粒掉落物

### 3. ExampleMod.java 注册
- 注册经验颗粒物品到ITEMS注册器
- 注册事件处理器到EVENT_BUS
- 添加到创造模式物品栏

## 工作流程

### 🔄 经验颗粒生成流程

1. **模拟升级触发**:
   ```java
   // MobSpawnerBlockEntity.simulateSpawn()
   SpawnerFakePlayer fakePlayer = new SpawnerFakePlayer(level, spawnerPos);
   fakePlayer.setupFakeWeapon(lootingLevel, beheadingLevel);
   ```

2. **击杀生物**:
   ```java
   // MobSpawnerBlockEntity.killEntityWithFakePlayer()
   LivingDropsEvent dropsEvent = new LivingDropsEvent(entity, damageSource, dropEntities, true);
   NeoForge.EVENT_BUS.post(dropsEvent);
   ```

3. **经验颗粒掉落**:
   ```java
   // ExperienceGranuleDropEvent.onLivingDrops()
   int baseExperience = ExperienceFluidHelper.getExperienceFromEntity(entity);
   int finalExperience = calculateExperienceWithLooting(baseExperience, lootingLevel, random);
   ItemStack experienceGranule = ExperienceGranuleItem.createWithExperience(finalExperience);
   ```

### 🎯 经验颗粒使用流程

1. **右键使用**:
   ```java
   // ExperienceGranuleItem.use()
   int experienceValue = getExperienceValue(itemStack);
   boolean isShiftPressed = player.isShiftKeyDown();
   int itemsToConsume = isShiftPressed ? itemStack.getCount() : 1;
   ```

2. **生成经验球**:
   ```java
   for (int i = 0; i < itemsToConsume; i++) {
       ExperienceOrb experienceOrb = new ExperienceOrb(level, 
           player.getX(), player.getY() + 0.5, player.getZ(), experienceValue);
       level.addFreshEntity(experienceOrb);
   }
   ```

## 特性

### ✅ 抢夺附魔影响
- 每级抢夺有25%概率额外获得1点经验
- 最大支持16级抢夺升级模块

### ✅ 经验值存储
- 使用NBT数据存储经验值
- 支持自定义经验值
- 工具提示显示经验值

### ✅ 批量使用
- 右键使用单个经验颗粒
- Shift+右键使用整个堆叠
- 播放经验球拾取音效

### ✅ 视觉效果
- 经验颗粒具有附魔光效
- 生成的经验球会自动被玩家吸收

## 经验值计算

### 📊 基础经验值
| 生物类型 | 经验值 |
|---------|--------|
| 僵尸/骷髅/爬行者 | 5 |
| 末影人 | 5 |
| 凋零骷髅 | 5 |
| 动物 | 1-3 (随机) |

### 🎲 抢夺影响
```java
// 每级抢夺25%概率额外+1经验
for (int i = 0; i < lootingLevel; i++) {
    if (random.nextFloat() < 0.25f) {
        bonusExperience++;
    }
}
```

## 使用方法

### 🏗️ 设置模拟升级
1. 在刷怪器中安装模拟升级模块
2. 可选：安装抢夺升级模块增加经验掉落
3. 刷怪器会自动击杀生物并掉落经验颗粒

### 🎮 使用经验颗粒
1. 右键使用单个经验颗粒获得经验
2. Shift+右键使用整个堆叠获得大量经验
3. 经验球会在玩家位置生成并自动被吸收

## 兼容性

### ✅ 与现有系统兼容
- 与原版经验系统完全兼容
- 与其他模组的经验修改兼容
- 与现有的模拟升级系统无缝集成

### ✅ 事件驱动设计
- 使用NeoForge事件系统
- 不修改原版核心逻辑
- 易于扩展和维护

## 配置

经验颗粒的行为可以通过以下方式调整：

1. **基础经验值**: 修改`ExperienceFluidHelper.getExperienceFromEntity()`
2. **抢夺影响**: 修改`ExperienceGranuleDropEvent.calculateExperienceWithLooting()`
3. **堆叠大小**: 在物品注册时修改`.stacksTo(64)`

## 测试建议

1. **基础功能测试**:
   - 创建经验颗粒物品
   - 右键使用验证经验球生成
   - Shift+右键验证批量使用

2. **模拟升级测试**:
   - 设置带模拟升级的刷怪器
   - 验证击杀生物时掉落经验颗粒
   - 测试不同抢夺等级的影响

3. **边界情况测试**:
   - 测试空手使用
   - 测试创造模式使用
   - 测试不同生物类型的经验值
