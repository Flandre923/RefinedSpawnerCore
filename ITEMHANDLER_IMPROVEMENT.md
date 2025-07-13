# ItemHandler 容器插入改进

## 问题分析

你提出的问题很准确：**掉落物没有进容器，而是掉到世界上去了**。

### 原因分析

1. **实体真正死亡**: 之前的实现调用了 `entity.die(damageSource)`，这会让实体在世界中正常死亡并掉落物品到地面
2. **容器插入失效**: 虽然有容器插入逻辑，但因为实体已经死亡掉落物品，我们的插入逻辑没有机会执行

## 解决方案

### ✅ 修复核心问题

1. **避免实体真正死亡**:
   ```java
   // 修改前（会导致实体死亡掉落物品）
   entity.die(damageSource);
   
   // 修改后（只生成掉落物，不让实体死亡）
   generateDropsFromLootTable(entity, fakePlayer, level, dropEntities);
   entity.discard(); // 移除实体但不触发死亡掉落
   ```

2. **使用NeoForge ItemHandler**:
   ```java
   // 修改前（使用原始Container接口）
   List<Container> containers = findNearbyContainers(level, spawnerPos);
   
   // 修改后（使用NeoForge ItemHandler）
   List<IItemHandler> itemHandlers = findNearbyItemHandlers(level, spawnerPos);
   ```

### 🔧 改进的实现

#### 1. 战利品表生成
```java
private void generateDropsFromLootTable(LivingEntity entity, SpawnerFakePlayer fakePlayer, ServerLevel level, List<ItemEntity> dropEntities) {
    // 获取实体的战利品表
    Optional<ResourceKey<LootTable>> lootTableKey = entity.getLootTable();
    LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey.get());
    
    // 创建战利品上下文，包含FakePlayer和武器信息
    LootParams.Builder builder = new LootParams.Builder(level)
        .withParameter(LootContextParams.THIS_ENTITY, entity)
        .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(fakePlayer))
        .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, fakePlayer);
    
    // 添加FakePlayer的武器作为工具参数（抢夺附魔生效）
    ItemStack weapon = fakePlayer.getMainHandItem();
    if (!weapon.isEmpty()) {
        builder.withParameter(LootContextParams.TOOL, weapon);
    }
    
    // 生成掉落物
    List<ItemStack> lootDrops = lootTable.getRandomItems(lootParams);
}
```

#### 2. ItemHandler 查找
```java
private List<IItemHandler> findNearbyItemHandlers(ServerLevel level, BlockPos spawnerPos) {
    List<IItemHandler> itemHandlers = new ArrayList<>();
    int searchRange = SpawnerModuleConfig.SIMULATION_CONTAINER_SEARCH_RANGE;
    
    for (int x = -searchRange; x <= searchRange; x++) {
        for (int y = -searchRange; y <= searchRange; y++) {
            for (int z = -searchRange; z <= searchRange; z++) {
                BlockPos checkPos = spawnerPos.offset(x, y, z);
                
                // 使用NeoForge Capability系统查找ItemHandler
                IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, checkPos, null);
                if (itemHandler != null) {
                    itemHandlers.add(itemHandler);
                }
            }
        }
    }
    
    return itemHandlers;
}
```

#### 3. 物品插入
```java
private List<ItemStack> insertItemsIntoItemHandler(IItemHandler handler, List<ItemStack> items) {
    List<ItemStack> remainingItems = new ArrayList<>();
    
    for (ItemStack stack : items) {
        // 使用NeoForge的ItemHandlerHelper进行智能插入
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        if (!remaining.isEmpty()) {
            remainingItems.add(remaining);
        }
    }
    
    return remainingItems;
}
```

## 优势

### ✅ NeoForge ItemHandler 的优势

1. **更好的兼容性**: 支持所有实现了ItemHandler的容器
2. **智能插入**: `ItemHandlerHelper.insertItemStacked` 会自动处理堆叠和分配
3. **性能优化**: 直接使用Capability系统，避免类型检查
4. **扩展性**: 支持模组添加的自定义容器

### ✅ 修复后的流程

```java
// 1. 创建临时实体（不在世界中生成）
LivingEntity livingEntity = (LivingEntity) tempEntity;
livingEntity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

// 2. 创建FakePlayer并设置假武器
SpawnerFakePlayer fakePlayer = new SpawnerFakePlayer(level, spawnerPos);
fakePlayer.setupFakeWeapon(lootingLevel, beheadingLevel);

// 3. 使用战利品表生成掉落物（不让实体真正死亡）
List<ItemStack> drops = killEntityWithFakePlayer(livingEntity, fakePlayer, level);

// 4. 使用ItemHandler插入到邻近容器
boolean inserted = insertDropsIntoContainers(level, spawnerPos, drops);

// 5. 剩余物品掉落到地面（如果容器满了）
```

## 支持的容器类型

### ✅ 原版容器
- 箱子 (Chest)
- 桶 (Barrel) 
- 潜影盒 (Shulker Box)
- 漏斗 (Hopper)
- 投掷器 (Dispenser)
- 发射器 (Dropper)

### ✅ 模组容器
- 任何实现了 `IItemHandler` 的容器
- 通过NeoForge Capability系统注册的容器

## 测试建议

1. **放置不同类型的容器**在刷怪器周围
2. **安装模拟升级模块**
3. **观察掉落物是否正确插入容器**
4. **测试容器满了的情况**（剩余物品应该掉落到地面）

## 总结

现在的实现：
- ✅ **修复了掉落物问题**: 不再让实体真正死亡
- ✅ **使用NeoForge ItemHandler**: 更好的容器兼容性
- ✅ **智能物品插入**: 自动处理堆叠和分配
- ✅ **保持抢夺机制**: FakePlayer的武器附魔正确生效
- ✅ **优雅降级**: 容器满了时物品掉落到地面

掉落物现在应该能够正确插入到邻近的容器中了！
