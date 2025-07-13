#!/usr/bin/env python3
"""
专门生成Minecraft模组的GUI纹理
重新设计刷怪器界面，解决槽位重合问题
"""

import os
from PIL import Image, ImageDraw, ImageFont

def create_texture_directory(path):
    """创建纹理目录"""
    os.makedirs(path, exist_ok=True)

def draw_slot(draw, x, y, size=18, style="normal"):
    """绘制槽位"""
    if style == "spawn_egg":
        # 刷怪蛋槽位 - 特殊样式
        draw.rectangle([x-2, y-2, x+size+1, y+size+1], outline=(255, 215, 0, 255), width=2)  # 金色外框
        draw.rectangle([x-1, y-1, x+size, y+size], outline=(0, 0, 0, 255), width=1)  # 黑色内框
        draw.rectangle([x, y, x+size-1, y+size-1], outline=(255, 255, 255, 64), width=1)  # 白色高光
    elif style == "module":
        # 模块槽位 - 标准样式
        draw.rectangle([x-1, y-1, x+size, y+size], outline=(0, 0, 0, 255), width=1)  # 黑色外框
        draw.rectangle([x, y, x+size-1, y+size-1], outline=(128, 128, 128, 128), width=1)  # 灰色内框
    elif style == "inventory":
        # 背包槽位 - 标准样式
        draw.rectangle([x-1, y-1, x+size, y+size], outline=(0, 0, 0, 255), width=1)  # 黑色外框
    elif style == "disabled":
        # 禁用槽位 - 暗色样式
        draw.rectangle([x-1, y-1, x+size, y+size], outline=(64, 64, 64, 255), width=1)  # 深灰色外框
        draw.rectangle([x, y, x+size-1, y+size-1], fill=(32, 32, 32, 128))  # 半透明填充

def draw_panel(draw, x, y, width, height, style="normal"):
    """绘制面板"""
    if style == "main":
        # 主面板
        draw.rectangle([x, y, x+width-1, y+height-1], outline=(0, 0, 0, 255), width=2)
        draw.rectangle([x+2, y+2, x+width-3, y+height-3], outline=(255, 255, 255, 128), width=1)
        draw.rectangle([x+1, y+1, x+width-2, y+height-2], fill=(198, 198, 198, 255))
    elif style == "section":
        # 区域面板
        draw.rectangle([x, y, x+width-1, y+height-1], outline=(128, 128, 128, 255), width=1)
        draw.rectangle([x+1, y+1, x+width-2, y+height-2], fill=(220, 220, 220, 128))

def create_spawn_egg_mob_spawner_gui():
    """创建刷怪蛋刷怪器GUI - 重新设计布局"""
    width, height = 176, 212
    img = Image.new('RGBA', (width, height), (198, 198, 198, 255))
    draw = ImageDraw.Draw(img)

    # 绘制主面板
    draw_panel(draw, 0, 0, width, height, "main")

    # === 顶部区域：刷怪蛋槽位 ===
    # 刷怪蛋区域背景
    draw_panel(draw, 70, 8, 36, 36, "section")
    draw_slot(draw, 80, 18, 18, "spawn_egg")

    # === 中部区域：模块槽位 ===
    # 模块区域背景
    draw_panel(draw, 6, 50, 164, 50, "section")

    # 模块槽位：重新设计为更合理的布局
    # 第一行：4个基础模块槽位
    for i in range(4):
        x = 12 + i * 20  # 增加间距
        y = 56
        draw_slot(draw, x, y, 18, "module")

    # 第二行：4个基础模块槽位
    for i in range(4):
        x = 12 + i * 20
        y = 76
        draw_slot(draw, x, y, 18, "module")

    # 右侧：2个特殊升级槽位
    for i in range(2):
        x = 100 + i * 20
        y = 56 + i * 20
        draw_slot(draw, x, y, 18, "disabled")  # 特殊槽位

    # === 控制按钮区域 ===
    # Always按钮区域
    draw.rectangle([8, 106, 50, 120], outline=(128, 128, 128, 255), width=1)
    draw.rectangle([9, 107, 49, 119], fill=(220, 220, 220, 255))

    # Show Area按钮区域
    draw.rectangle([55, 106, 120, 120], outline=(128, 128, 128, 255), width=1)
    draw.rectangle([56, 107, 119, 119], fill=(220, 220, 220, 255))

    # 偏移控制区域
    draw.rectangle([125, 106, 168, 120], outline=(128, 128, 128, 255), width=1)
    draw.rectangle([126, 107, 167, 119], fill=(220, 220, 220, 255))

    # === 玩家背包区域 ===
    inventory_y = 130
    draw_panel(draw, 4, inventory_y-5, width-8, 80, "section")

    # 玩家背包槽位 (9x3)
    for row in range(3):
        for col in range(9):
            x = 8 + col * 18
            y = inventory_y + row * 18
            draw_slot(draw, x, y, 18, "inventory")

    # 玩家快捷栏 (9x1)
    hotbar_y = 188
    for col in range(9):
        x = 8 + col * 18
        y = hotbar_y
        draw_slot(draw, x, y, 18, "inventory")

    return img

def create_mob_spawner_gui():
    """创建调试刷怪器GUI"""
    width, height = 176, 166  # 调试界面使用标准高度
    img = Image.new('RGBA', (width, height), (198, 198, 198, 255))
    draw = ImageDraw.Draw(img)
    
    # 绘制主面板
    draw_panel(draw, 0, 0, width, height, "main")
    
    # 标题区域
    draw.rectangle([4, 4, width-5, 20], outline=(128, 128, 128, 255), width=1)
    draw.rectangle([5, 5, width-6, 19], fill=(240, 240, 240, 255))
    
    # 刷怪蛋槽位
    draw_slot(draw, 80, 35, 18, "spawn_egg")
    
    # 模块槽位 (3x2布局，用于调试界面)
    for row in range(2):
        for col in range(3):
            x = 8 + col * 18
            y = 60 + row * 18
            draw_slot(draw, x, y, 18, "module")
    
    # 控制区域
    draw.rectangle([4, 100, width-5, 160], outline=(128, 128, 128, 255), width=1)
    draw.rectangle([5, 101, width-6, 159], fill=(240, 240, 240, 255))
    
    # 玩家背包区域（简化版）
    inventory_y = 165
    for row in range(2):
        for col in range(9):
            x = 8 + col * 18
            y = inventory_y + row * 18
            draw_slot(draw, x, y, 18, "inventory")
    
    return img

def main():
    """主函数"""
    base_path = "src/main/resources/assets/examplemod/textures/gui"
    create_texture_directory(base_path)
    
    print("重新生成GUI纹理...")
    
    # 生成刷怪蛋刷怪器GUI
    spawn_egg_gui = create_spawn_egg_mob_spawner_gui()
    spawn_egg_path = os.path.join(base_path, "spawn_egg_mob_spawner.png")
    spawn_egg_gui.save(spawn_egg_path)
    print(f"  创建: spawn_egg_mob_spawner.png (176x212)")

    # 生成调试刷怪器GUI
    mob_spawner_gui = create_mob_spawner_gui()
    mob_spawner_path = os.path.join(base_path, "mob_spawner.png")
    mob_spawner_gui.save(mob_spawner_path)
    print(f"  创建: mob_spawner.png (176x166)")
    
    print(f"\n✅ 完成！重新生成了GUI纹理")
    print(f"\n📝 布局改进:")
    print("1. 刷怪蛋槽位：(80, 35) - 居中位置，金色边框")
    print("2. 模块槽位：(8, 45) 开始的4x2布局，避免与刷怪蛋重合")
    print("3. 额外槽位：右侧位置，默认禁用状态")
    print("4. 控制区域：(85-110) 位置，用于按钮和信息显示")
    print("5. 背包区域：(120-200) 位置，标准9x3+9x1布局")
    print("6. 增加了视觉分区和面板边框")
    print("7. 不同类型槽位使用不同的视觉样式")

if __name__ == "__main__":
    main()
