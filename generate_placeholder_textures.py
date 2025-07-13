#!/usr/bin/env python3
"""
生成Minecraft模组的32x32纯色占位纹理图片
"""

import os
from PIL import Image, ImageDraw, ImageFont
import colorsys

def create_texture_directory(path):
    """创建纹理目录"""
    os.makedirs(path, exist_ok=True)

def generate_color_from_name(name):
    """根据名称生成一个独特的颜色"""
    # 使用名称的哈希值生成颜色
    hash_value = hash(name) % 360
    # 使用HSV颜色空间生成饱和度较高的颜色
    h = hash_value / 360.0
    s = 0.7  # 饱和度
    v = 0.8  # 明度
    
    rgb = colorsys.hsv_to_rgb(h, s, v)
    return tuple(int(c * 255) for c in rgb)

def create_placeholder_texture(name, color, size=(32, 32), text=None):
    """创建占位纹理"""
    # 创建图像
    img = Image.new('RGBA', size, color + (255,))
    
    # 如果提供了文本，添加文本
    if text:
        draw = ImageDraw.Draw(img)
        try:
            # 尝试使用默认字体
            font = ImageFont.load_default()
        except:
            font = None
        
        # 计算文本位置（居中）
        if font:
            bbox = draw.textbbox((0, 0), text, font=font)
            text_width = bbox[2] - bbox[0]
            text_height = bbox[3] - bbox[1]
        else:
            text_width = len(text) * 6
            text_height = 11
        
        x = (size[0] - text_width) // 2
        y = (size[1] - text_height) // 2
        
        # 绘制文本（白色，带黑色边框）
        for dx in [-1, 0, 1]:
            for dy in [-1, 0, 1]:
                if dx != 0 or dy != 0:
                    draw.text((x + dx, y + dy), text, fill=(0, 0, 0, 255), font=font)
        draw.text((x, y), text, fill=(255, 255, 255, 255), font=font)
    
    return img

def main():
    """主函数"""
    base_path = "src/main/resources/assets/examplemod/textures"
    
    # 物品纹理定义
    item_textures = {
        # 基础物品
        "example_item": {"color": (100, 150, 200), "text": "EX"},
        "experience_granule": {"color": (255, 215, 0), "text": "XP"},
        "magic_water_bucket": {"color": (138, 43, 226), "text": "MW"},
        "experience_bucket": {"color": (0, 255, 127), "text": "EB"},

        # 刷怪器模块
        "range_reducer_module": {"color": (255, 100, 100), "text": "R-"},
        "range_expander_module": {"color": (100, 255, 100), "text": "R+"},
        "min_delay_reducer_module": {"color": (100, 100, 255), "text": "MD"},
        "max_delay_reducer_module": {"color": (255, 255, 100), "text": "XD"},
        "count_booster_module": {"color": (255, 100, 255), "text": "C+"},
        "player_ignorer_module": {"color": (100, 255, 255), "text": "PI"},
        "simulation_upgrade_module": {"color": (200, 100, 50), "text": "SU"},
        "looting_upgrade_module": {"color": (50, 200, 100), "text": "LT"},
        "beheading_upgrade_module": {"color": (200, 50, 100), "text": "BH"},
    }
    
    # 方块纹理定义
    block_textures = {
        "example_block": {"color": (150, 150, 150), "text": "EB"},
        "mob_spawner": {"color": (64, 64, 64), "text": "MS"},
        "fluid_tank": {"color": (192, 192, 192), "text": "FT"},
    }
    
    # GUI纹理定义（176x166或176x202）
    gui_textures = {
        "mob_spawner": {"size": (176, 202), "color": (139, 139, 139)},
        "spawn_egg_mob_spawner": {"size": (176, 166), "color": (169, 169, 169)},
    }
    
    # 创建物品纹理
    item_dir = os.path.join(base_path, "item")
    create_texture_directory(item_dir)
    
    print("生成物品纹理...")
    for name, config in item_textures.items():
        color = config.get("color")
        if not color:
            color = generate_color_from_name(name)
        
        text = config.get("text", name[:2].upper())
        img = create_placeholder_texture(name, color, text=text)
        
        filepath = os.path.join(item_dir, f"{name}.png")
        img.save(filepath)
        print(f"  创建: {filepath}")
    
    # 创建方块纹理
    block_dir = os.path.join(base_path, "block")
    create_texture_directory(block_dir)
    
    print("\n生成方块纹理...")
    for name, config in block_textures.items():
        color = config.get("color")
        if not color:
            color = generate_color_from_name(name)
        
        text = config.get("text", name[:2].upper())
        img = create_placeholder_texture(name, color, text=text)
        
        filepath = os.path.join(block_dir, f"{name}.png")
        img.save(filepath)
        print(f"  创建: {filepath}")
    
    # 创建GUI纹理
    gui_dir = os.path.join(base_path, "gui")
    create_texture_directory(gui_dir)
    
    print("\n生成GUI纹理...")
    for name, config in gui_textures.items():
        size = config.get("size", (176, 166))
        color = config.get("color", (139, 139, 139))
        
        # GUI纹理使用简单的灰色背景，不添加文本
        img = create_placeholder_texture(name, color, size=size)
        
        # 为GUI添加一些基本的边框和槽位标记
        draw = ImageDraw.Draw(img)
        
        # 绘制边框
        draw.rectangle([0, 0, size[0]-1, size[1]-1], outline=(0, 0, 0, 255), width=2)
        draw.rectangle([2, 2, size[0]-3, size[1]-3], outline=(255, 255, 255, 128), width=1)
        
        # 为spawn_egg_mob_spawner GUI添加槽位标记
        if name == "spawn_egg_mob_spawner":
            # 刷怪蛋槽位 (80, 35) - 居中位置
            draw.rectangle([79, 34, 97, 52], outline=(0, 0, 0, 255), width=2)
            draw.rectangle([80, 35, 96, 51], outline=(255, 255, 255, 128), width=1)

            # 模块槽位：前8个槽位4x2布局 (8, 45)
            for i in range(8):
                x = 8 + (i % 4) * 18
                y = 45 + (i // 4) * 18
                draw.rectangle([x-1, y-1, x+17, y+17], outline=(0, 0, 0, 255), width=1)
                draw.rectangle([x, y, x+16, y+16], outline=(128, 128, 128, 128), width=1)

            # 额外2个槽位：右侧 (8 + 4*18 + 10, 45)
            for i in range(2):
                x = 8 + 4 * 18 + 10 + (i % 2) * 18
                y = 45 + (i // 2) * 18
                draw.rectangle([x-1, y-1, x+17, y+17], outline=(0, 0, 0, 255), width=1)
                draw.rectangle([x, y, x+16, y+16], outline=(128, 128, 128, 128), width=1)

            # 玩家背包槽位 (8, 120) - 9x3
            for row in range(3):
                for col in range(9):
                    x = 8 + col * 18
                    y = 120 + row * 18
                    draw.rectangle([x-1, y-1, x+17, y+17], outline=(0, 0, 0, 255), width=1)

            # 玩家快捷栏 (8, 178) - 9x1
            for col in range(9):
                x = 8 + col * 18
                y = 178
                draw.rectangle([x-1, y-1, x+17, y+17], outline=(0, 0, 0, 255), width=1)

        elif name == "mob_spawner":
            # 调试界面的刷怪蛋槽位 (80, 35)
            draw.rectangle([79, 34, 97, 52], outline=(0, 0, 0, 255), width=2)

            # 调试界面的模块槽位 3x2 grid starting at (8, 60)
            for row in range(2):
                for col in range(3):
                    x = 8 + col * 18
                    y = 60 + row * 18
                    draw.rectangle([x-1, y-1, x+17, y+17], outline=(0, 0, 0, 255), width=1)
        
        filepath = os.path.join(gui_dir, f"{name}.png")
        img.save(filepath)
        print(f"  创建: {filepath}")
    
    print(f"\n✅ 完成！生成了 {len(item_textures) + len(block_textures) + len(gui_textures)} 个占位纹理文件")
    print("\n📝 注意事项:")
    print("1. 这些是临时占位纹理，建议后续替换为正式的美术资源")
    print("2. GUI纹理包含了基本的槽位标记，可以直接使用")
    print("3. 所有纹理都使用了不同的颜色以便区分")
    print("4. 物品纹理包含了简短的文本标识")

if __name__ == "__main__":
    main()
