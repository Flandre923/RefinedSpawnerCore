# Items JSON文件实现

## 概述

在新版本的Minecraft (1.21.6) 中，除了传统的模型文件外，每个物品还需要在 `assets/modid/items/` 目录下有对应的JSON配置文件。这些文件定义了物品的模型引用和其他属性。

## 新版本要求

### 📋 必需文件结构
```
src/main/resources/assets/examplemod/
├── models/item/           # 传统模型文件
│   ├── example_item.json
│   └── ...
└── items/                 # 新版本要求的items JSON
    ├── example_item.json
    └── ...
```

### 🔗 文件关联
- **Items JSON** → 引用 **Models JSON** → 引用 **Texture PNG**
- 每个物品需要三个文件才能正确显示

## 生成的Items JSON文件

### 📊 统计信息
- **总计**: 16个items JSON文件
- **位置**: `src/main/resources/assets/examplemod/items/`
- **格式**: 符合Minecraft 1.21.6标准

### 📝 文件列表

| 物品类型 | 文件名 | 模型引用 |
|----------|--------|----------|
| **基础物品** | | |
| 示例物品 | `example_item.json` | `examplemod:item/example_item` |
| 经验颗粒 | `experience_granule.json` | `examplemod:item/experience_granule` |
| **方块物品** | | |
| 示例方块 | `example_block.json` | `examplemod:item/example_block` |
| 流体储罐 | `fluid_tank.json` | `examplemod:item/fluid_tank` |
| 刷怪器 | `mob_spawner.json` | `examplemod:item/mob_spawner` |
| **桶类物品** | | |
| 魔法水桶 | `magic_water_bucket.json` | `examplemod:item/magic_water_bucket` |
| 经验桶 | `experience_bucket.json` | `examplemod:item/experience_bucket` |
| **刷怪器模块** | | |
| 范围缩减模块 | `range_reducer_module.json` | `examplemod:item/range_reducer_module` |
| 范围扩展模块 | `range_expander_module.json` | `examplemod:item/range_expander_module` |
| 最小延迟缩减模块 | `min_delay_reducer_module.json` | `examplemod:item/min_delay_reducer_module` |
| 最大延迟缩减模块 | `max_delay_reducer_module.json` | `examplemod:item/max_delay_reducer_module` |
| 数量增强模块 | `count_booster_module.json` | `examplemod:item/count_booster_module` |
| 玩家忽略模块 | `player_ignorer_module.json` | `examplemod:item/player_ignorer_module` |
| 模拟升级模块 | `simulation_upgrade_module.json` | `examplemod:item/simulation_upgrade_module` |
| 抢夺升级模块 | `looting_upgrade_module.json` | `examplemod:item/looting_upgrade_module` |
| 斩首升级模块 | `beheading_upgrade_module.json` | `examplemod:item/beheading_upgrade_module` |

## JSON文件格式

### 📄 标准格式
每个items JSON文件都使用以下标准格式：

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "examplemod:item/物品名称"
  }
}
```

### 🔍 示例文件
**文件**: `src/main/resources/assets/examplemod/items/experience_granule.json`
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "examplemod:item/experience_granule"
  }
}
```

## 自动生成脚本

### 🛠️ 脚本功能
`generate_items_json.py` 脚本提供以下功能：

1. **自动检测**: 根据ExampleMod.java中注册的物品自动生成
2. **智能跳过**: 跳过已存在的文件，避免覆盖
3. **标准格式**: 生成符合Minecraft 1.21.6标准的JSON文件
4. **批量处理**: 一次性生成所有缺失的文件

### 📋 支持的物品类型
- ✅ 普通物品 (Item)
- ✅ 方块物品 (BlockItem)
- ✅ 桶类物品 (BucketItem)
- ✅ 自定义物品 (SpawnerModuleItem, ExperienceGranuleItem)

### 🚀 使用方法
```bash
# 运行脚本生成所有缺失的items JSON文件
python generate_items_json.py
```

## 版本兼容性

### 🆕 新版本特性
- **Minecraft 1.21.6**: 要求items JSON文件
- **NeoForge**: 完全兼容新的资源包格式
- **向后兼容**: 保持与旧版本模型文件的兼容性

### 🔄 迁移说明
如果从旧版本升级：
1. 保留现有的模型文件
2. 添加新的items JSON文件
3. 确保文件名和路径一致

## 故障排除

### ❌ 常见问题

1. **物品显示错误纹理**
   - 检查items JSON文件是否存在
   - 验证模型引用路径是否正确
   - 确认对应的模型文件和纹理文件存在

2. **JSON格式错误**
   - 使用JSON验证器检查语法
   - 确保使用正确的字段名称
   - 检查引号和逗号

3. **文件路径错误**
   - 确保文件在正确的目录下
   - 检查文件名是否与注册名称一致
   - 验证命名空间是否正确

### ✅ 验证清单
- [ ] 所有注册的物品都有对应的items JSON文件
- [ ] JSON文件格式正确
- [ ] 模型引用路径正确
- [ ] 对应的模型文件存在
- [ ] 对应的纹理文件存在

## 扩展和自定义

### 🎨 自定义属性
未来可以在items JSON中添加更多属性：
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "examplemod:item/example_item"
  },
  "tooltip_style": "custom",
  "use_duration": 32
}
```

### 🔧 批量修改
可以修改 `generate_items_json.py` 脚本来：
- 添加自定义属性
- 使用不同的模型类型
- 支持条件模型

## 总结

✅ **完成状态**:
- 16个items JSON文件已生成
- 所有文件格式正确
- 完全符合新版本要求
- 可以立即在游戏中使用

🎯 **效果**:
- 解决了新版本Minecraft的兼容性问题
- 确保所有物品正确显示
- 为未来扩展奠定基础

现在你的模组完全符合Minecraft 1.21.6的新版本要求！
