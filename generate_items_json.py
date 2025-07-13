#!/usr/bin/env python3
"""
生成Minecraft模组的items JSON文件
新版本Minecraft需要在assets/modid/items/目录下为每个物品创建JSON配置文件
"""

import os
import json

def create_items_directory(path):
    """创建items目录"""
    os.makedirs(path, exist_ok=True)

def create_item_json(item_name, model_path=None):
    """创建物品JSON配置"""
    if model_path is None:
        model_path = f"examplemod:item/{item_name}"
    
    return {
        "model": {
            "type": "minecraft:model",
            "model": model_path
        }
    }

def main():
    """主函数"""
    base_path = "src/main/resources/assets/examplemod/items"
    create_items_directory(base_path)
    
    # 根据ExampleMod.java中注册的物品定义items列表
    items = [
        # 基础物品
        "example_item",
        "experience_granule",
        
        # 方块物品 (BlockItem)
        "example_block",
        "fluid_tank",
        "mob_spawner",
        
        # 桶类物品
        "experience_bucket",
        # magic_water_bucket 已存在，跳过
        
        # 刷怪器模块
        "range_reducer_module",
        "range_expander_module", 
        "min_delay_reducer_module",
        "max_delay_reducer_module",
        "count_booster_module",
        "player_ignorer_module",
        "simulation_upgrade_module",
        "looting_upgrade_module",
        "beheading_upgrade_module",
    ]
    
    print("生成items JSON文件...")
    
    created_count = 0
    skipped_count = 0
    
    for item_name in items:
        filepath = os.path.join(base_path, f"{item_name}.json")
        
        # 检查文件是否已存在
        if os.path.exists(filepath):
            print(f"  跳过: {item_name}.json (已存在)")
            skipped_count += 1
            continue
        
        # 创建JSON内容
        item_json = create_item_json(item_name)
        
        # 写入文件
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(item_json, f, indent=2)
        
        print(f"  创建: {item_name}.json")
        created_count += 1
    
    print(f"\n✅ 完成！")
    print(f"📊 统计:")
    print(f"  - 创建了 {created_count} 个新文件")
    print(f"  - 跳过了 {skipped_count} 个已存在的文件")
    print(f"  - 总计 {len(items)} 个物品")
    
    print(f"\n📝 注意事项:")
    print("1. 这些JSON文件定义了物品的模型引用")
    print("2. 每个物品都指向对应的模型文件 (models/item/)")
    print("3. 新版本Minecraft要求每个物品都有对应的items JSON文件")
    print("4. 文件格式遵循Minecraft 1.21.6的标准")

if __name__ == "__main__":
    main()
