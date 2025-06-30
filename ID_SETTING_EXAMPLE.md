# NeoForge 1.21.6 ID设置示例

在NeoForge 1.21.6中，注册方块和物品时必须为Properties设置ID，否则会出现"Block id not set"或"Item id not set"错误。

## 错误示例 ❌

```java
// 这会导致 "Block id not set" 错误
public static final DeferredBlock<LiquidBlock> MAGIC_WATER_BLOCK = BLOCKS.register("magic_water",
    () -> new LiquidBlock(MAGIC_WATER.get(), BlockBehaviour.Properties.of()
        .mapColor(MapColor.WATER).replaceable().noCollission()));

// 这会导致 "Item id not set" 错误  
public static final DeferredItem<BucketItem> MAGIC_WATER_BUCKET = ITEMS.register("magic_water_bucket",
    () -> new BucketItem(MAGIC_WATER.get(), new Item.Properties()
        .craftRemainder(Items.BUCKET).stacksTo(1)));
```

## 正确示例 ✅

```java
// 正确的方块注册 - 必须设置ID
public static final DeferredBlock<LiquidBlock> MAGIC_WATER_BLOCK = BLOCKS.register("magic_water",
    () -> new LiquidBlock(MAGIC_WATER.get(), BlockBehaviour.Properties.of()
        .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "magic_water")))
        .mapColor(MapColor.WATER).replaceable().noCollission()));

// 正确的物品注册 - 必须设置ID
public static final DeferredItem<BucketItem> MAGIC_WATER_BUCKET = ITEMS.register("magic_water_bucket",
    () -> new BucketItem(MAGIC_WATER.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "magic_water_bucket")))
        .craftRemainder(Items.BUCKET).stacksTo(1)));
```

## 必需的导入

```java
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
```

## 模式说明

### 方块ID设置
```java
.setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "block_name")))
```

### 物品ID设置
```java
.setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "item_name")))
```

### 参数说明
- `MODID`: 你的模组ID (例如: "examplemod")
- `"block_name"` / `"item_name"`: 方块/物品的注册名称
- `Registries.BLOCK` / `Registries.ITEM`: 对应的注册表类型

## 注意事项

1. **ID必须唯一**: 每个方块/物品的ID在整个游戏中必须是唯一的
2. **命名规范**: 使用小写字母和下划线，避免特殊字符
3. **一致性**: 注册名称应该与ID中的名称保持一致
4. **模组命名空间**: 使用你的模组ID作为命名空间，避免与其他模组冲突

这个变化是NeoForge 1.21.6的新要求，确保所有方块和物品都有明确的标识符。
