# FakePlayer 模拟升级实现

## 概述

本实现通过FakePlayer和假武器的方式来触发原版的掠夺机制，替代了之前直接操作战利品表的方式。这样可以更好地兼容原版机制和其他模组。

## 核心组件

### 1. 假武器 (使用原版铁剑)
- **实现**: 直接使用 `Items.IRON_SWORD`
- **功能**:
  - 使用原版铁剑作为FakePlayer的主手武器
  - 可以附加抢夺附魔来触发原版掠夺机制
  - 简化实现，避免自定义物品的复杂性
  - 通过 `mainHand.is(Items.IRON_SWORD)` 检查

### 2. SpawnerFakePlayer (假玩家)
- **文件**: `src/main/java/com/example/examplemod/util/SpawnerFakePlayer.java`
- **功能**:
  - 继承自NeoForge的FakePlayer
  - 持有带有抢夺附魔的假武器
  - 存储斩首等级信息
  - 模拟玩家击杀生物的行为

### 3. EntityHeadDropEvent (头颅掉落事件处理)
- **文件**: `src/main/java/com/example/examplemod/event/EntityHeadDropEvent.java`
- **功能**:
  - 监听LivingDropsEvent事件
  - 检测SpawnerFakePlayer的击杀
  - 根据斩首等级计算头颅掉落概率
  - 支持原版头颅类型（僵尸、骷髅、爬行者、凋零骷髅、玩家等）

## 实现流程

### 模拟升级触发流程

1. **设备击杀生物时**:
   ```java
   // 在 MobSpawnerBlockEntity.simulateSpawn() 中
   SpawnerFakePlayer fakePlayer = new SpawnerFakePlayer(level, spawnerPos);
   int lootingLevel = this.moduleManager.getLootingLevel();
   int beheadingLevel = this.moduleManager.getBeheadingLevel();
   fakePlayer.setupFakeWeapon(lootingLevel, beheadingLevel);
   ```

2. **设置假武器**:
   ```java
   // 在 SpawnerFakePlayer.setupFakeWeapon() 中
   ItemStack fakeWeapon = new ItemStack(Items.IRON_SWORD);
   if (lootingLevel > 0) {
       fakeWeapon.enchant(Enchantments.LOOTING, lootingLevel);
   }
   this.setItemSlot(EquipmentSlot.MAINHAND, fakeWeapon);
   ```

3. **使用FakePlayer击杀**:
   ```java
   // 在 MobSpawnerBlockEntity.killEntityWithFakePlayer() 中
   DamageSource damageSource = level.damageSources().playerAttack(fakePlayer);
   entity.setHealth(0.0F);
   entity.dropFromLootTable(level, damageSource, true);
   
   LivingDropsEvent dropsEvent = new LivingDropsEvent(entity, damageSource, dropEntities, 0, true);
   NeoForge.EVENT_BUS.post(dropsEvent);
   ```

4. **原版掠夺机制自动触发**:
   - Minecraft原版的掠夺算法自动读取FakePlayer主手武器的Looting等级
   - 自动提升掉落物数量和稀有掉落概率

5. **斩首效果处理**:
   ```java
   // 在 EntityHeadDropEvent.dropEvent() 中
   if (event.getSource().getEntity() instanceof SpawnerFakePlayer fakePlayer) {
       int beheadingLevel = fakePlayer.getBeheadingLevel();
       if (beheadingLevel > 0) {
           int dropChance = random.nextInt(10);
           if (dropChance < beheadingLevel) {
               // 添加头颅掉落
           }
       }
   }
   ```

## 关键优势

1. **兼容性**: 使用原版掠夺机制，与其他模组兼容性更好
2. **扩展性**: 可以轻松添加其他附魔效果
3. **一致性**: 掠夺效果与玩家手动击杀完全一致
4. **事件驱动**: 通过事件系统处理斩首效果，便于扩展

## 注册和配置

### 物品注册
```java
// 在 ExampleMod.java 中
public static final DeferredItem<InvisibleSwordItem> INVISIBLE_SWORD = 
    ITEMS.register("invisible_sword", () -> new InvisibleSwordItem());
```

### 事件处理器注册
```java
// 在 ExampleMod 构造函数中
NeoForge.EVENT_BUS.register(new EntityHeadDropEvent());
```

## 使用方式

模拟升级模块安装后，刷怪器会自动：
1. 创建SpawnerFakePlayer
2. 根据升级数量设置抢夺和斩首等级
3. 使用FakePlayer击杀生物
4. 触发原版掠夺机制和自定义斩首机制
5. 将掉落物插入到周围容器中

## 修复的问题

### 编译错误修复

1. **InvisibleSwordItem 类设计**:
   - 修复了 `extends` 后缺少父类名的语法错误
   - 简化为继承Item而不是SwordItem，避免版本兼容性问题
   - 移除了复杂的属性修饰符代码

2. **API兼容性问题**:
   - 修复了LivingDropsEvent构造函数参数不匹配的问题
   - 替换了不可访问的dropFromLootTable方法为die方法
   - 修复了getCommandSenderWorld()方法不存在的问题

3. **导入和注册问题**:
   - 添加了缺失的导入：`ItemEntity`, `ArrayList`, `InvisibleSwordItem`
   - 修复了DeferredItem泛型类型问题
   - 使用注册的物品实例而不是直接new

### 代码优化

```java
// 修复前（有语法错误和API问题）
public class InvisibleSwordItem extends  {
    public InvisibleSwordItem() {
        super(Tiers.IRON, new Item.Properties()
            .attributes(createAttributes())
            .stacksTo(1)
        );
    }
}

// 修复后（简化且兼容的设计）
public class InvisibleSwordItem extends Item {
    public InvisibleSwordItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean isInvisibleSword(ItemStack stack) {
        return stack.getItem() instanceof InvisibleSwordItem;
    }
}
```

### API调用修复

```java
// 修复前（API不兼容）
LivingDropsEvent dropsEvent = new LivingDropsEvent(entity, damageSource, dropEntities, 0, true);
entity.dropFromLootTable(level, damageSource, true);
int dropChance = event.getEntity().getCommandSenderWorld().random.nextInt(10);

// 修复后（正确的API调用）
LivingDropsEvent dropsEvent = new LivingDropsEvent(entity, damageSource, dropEntities, true);
entity.die(damageSource);
int dropChance = event.getEntity().level().random.nextInt(10);
```

### 注册修复

```java
// 修复前（泛型类型错误）
public static final DeferredItem<InvisibleSwordItem> INVISIBLE_SWORD =
    ITEMS.register("invisible_sword", () -> new InvisibleSwordItem());

// 修复后（正确的泛型类型）
public static final DeferredItem<Item> INVISIBLE_SWORD =
    ITEMS.register("invisible_sword", () -> new InvisibleSwordItem());
```

## 扩展建议

1. **自定义附魔**: 可以创建真正的斩首附魔注册到游戏中
2. **更多头颅类型**: 支持模组生物的头颅掉落
3. **配置选项**: 添加配置文件来调整掉落概率
4. **统计信息**: 记录击杀数量和掉落统计
