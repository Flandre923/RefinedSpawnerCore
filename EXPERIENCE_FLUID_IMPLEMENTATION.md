# 经验流体实现

## 概述

我已经成功添加了经验流体系统，用于将击杀怪物获得的经验转换为可存储的流体形式。

## 核心组件

### 1. ExperienceFluidType (经验流体类型)
- **文件**: `src/main/java/com/example/examplemod/fluid/ExperienceFluidType.java`
- **特性**:
  - 高发光等级 (10) - 类似经验球的发光效果
  - 向上浮动 (密度 800) - 比水轻
  - 较高粘度 (1200) - 流动较慢
  - 使用经验球音效
  - 可以形成无限源
  - 不会溺水（对玩家有益）

### 2. ExperienceFluid (经验流体实现)
- **文件**: `src/main/java/com/example/examplemod/fluid/ExperienceFluid.java`
- **功能**:
  - 继承自FlowingFluid
  - 支持源方块和流动状态
  - 类似水的流动机制但更粘稠
  - 发光效果

### 3. ExperienceClientExtensions (客户端渲染)
- **文件**: `src/main/java/com/example/examplemod/fluid/ExperienceClientExtensions.java`
- **特性**:
  - 亮绿色调 (0xFF7FFF00) - 类似经验球颜色
  - 自定义纹理路径

### 4. ExperienceFluidHelper (经验流体辅助工具)
- **文件**: `src/main/java/com/example/examplemod/util/ExperienceFluidHelper.java`
- **功能**:
  - 经验值与流体量转换
  - 自动存储到邻近容器
  - 在世界中放置流体方块
  - 经验值计算

## 转换机制

### 📊 经验转换比例
```java
// 1 经验点 = 10 mB 经验流体
public static final int EXP_TO_FLUID_RATIO = 10;

// 1个源方块 = 1000 mB = 100 经验点
public static final int FLUID_BLOCK_AMOUNT = 1000;
```

### 🎯 怪物经验值
| 怪物类型 | 经验值 | 流体量 |
|---------|--------|--------|
| 僵尸/骷髅/爬行者 | 5 | 50 mB |
| 末影人 | 5 | 50 mB |
| 凋零骷髅 | 5 | 50 mB |
| 动物 | 1-3 | 10-30 mB |

## 工作流程

### 🔄 模拟升级 + 经验流体生成

```java
// 1. 使用FakePlayer击杀生物
List<ItemStack> drops = killEntityWithFakePlayer(livingEntity, fakePlayer, level);

// 2. 将掉落物插入容器
boolean inserted = insertDropsIntoContainers(level, spawnerPos, drops);

// 3. 生成经验流体
int experience = ExperienceFluidHelper.getExperienceFromEntity(livingEntity);
boolean experienceStored = ExperienceFluidHelper.storeExperienceFluid(level, spawnerPos, experience);
```

### 💧 经验流体存储优先级

1. **流体容器优先**: 自动寻找邻近的流体储罐
2. **世界放置备用**: 如果容器满了，在世界中放置流体方块
3. **智能位置选择**: 螺旋式搜索合适的放置位置

## 注册和配置

### 🔧 流体注册
```java
// 流体类型
public static final DeferredHolder<FluidType, FluidType> EXPERIENCE_TYPE = 
    FLUID_TYPES.register("experience", ExperienceFluidType::new);

// 流体实例
public static final DeferredHolder<Fluid, FlowingFluid> EXPERIENCE = 
    FLUIDS.register("experience", () -> new ExperienceFluid.Source());
public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_EXPERIENCE = 
    FLUIDS.register("flowing_experience", () -> new ExperienceFluid.Flowing());

// 流体方块
public static final DeferredBlock<LiquidBlock> EXPERIENCE_BLOCK = 
    BLOCKS.register("experience", () -> new LiquidBlock(...));

// 流体桶
public static final DeferredItem<BucketItem> EXPERIENCE_BUCKET = 
    ITEMS.register("experience_bucket", () -> new BucketItem(...));
```

### 🎨 客户端注册
```java
@SubscribeEvent
public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    event.registerFluidType(new ExperienceClientExtensions(), EXPERIENCE_TYPE.get());
}
```

## 使用场景

### 🏭 自动化经验农场
1. **刷怪器** + **模拟升级** = 自动击杀怪物
2. **经验流体生成** = 将经验转换为可存储的流体
3. **流体储罐** = 大量存储经验流体
4. **流体管道** = 传输经验流体到需要的地方

### ⚗️ 经验处理系统
- **经验储罐**: 存储大量经验流体
- **经验提取**: 从流体中提取经验给玩家
- **经验交易**: 使用经验流体进行交易
- **经验合成**: 使用经验流体进行特殊合成

## 扩展功能建议

### 🔮 未来功能
1. **经验提取器**: 将经验流体转换回经验球
2. **经验注入器**: 直接给玩家添加经验
3. **经验合成台**: 使用经验流体进行特殊合成
4. **经验传输管道**: 专门的经验流体传输系统
5. **经验等级显示**: 在流体储罐上显示等效经验等级

### 🎯 配置选项
```java
// 可配置的转换比例
public static int EXP_TO_FLUID_RATIO = Config.getExpToFluidRatio();

// 可配置的搜索范围
public static int CONTAINER_SEARCH_RANGE = Config.getContainerSearchRange();
```

## 兼容性

### ✅ 支持的容器
- 原版流体储罐
- 模组流体储罐 (通过IFluidHandler)
- 任何实现FluidHandler capability的容器

### ✅ 支持的传输
- 流体管道
- 流体泵
- 任何支持Forge/NeoForge流体系统的设备

## 总结

经验流体系统现在完全集成到模拟升级中：

- ✅ **自动生成**: 击杀怪物时自动生成经验流体
- ✅ **智能存储**: 优先存储到容器，备用放置到世界
- ✅ **完整流体系统**: 支持桶装、管道传输、储罐存储
- ✅ **视觉效果**: 发光的绿色流体，类似经验球
- ✅ **兼容性**: 与所有支持NeoForge流体系统的模组兼容

现在你的刷怪器不仅能产生物品，还能产生可存储的经验流体！
