package com.example.examplemod.redstone;

import net.minecraft.util.StringRepresentable;

/**
 * 红石控制模式枚举
 * 定义刷怪器如何响应红石信号
 */
public enum RedstoneMode implements StringRepresentable {
    ALWAYS("always", "redstone.mode.always", "always"),
    REDSTONE_OFF("redstone_off", "redstone.mode.no_signal", "no_signal"),
    REDSTONE_ON("redstone_on", "redstone.mode.with_signal", "with_signal");

    private final String name;
    private final String displayName;
    private final String description;

    RedstoneMode(String name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * 检查在给定红石信号强度下是否应该工作
     * 
     * @param redstonePower 红石信号强度 (0-15)
     * @return 如果应该工作返回true，否则返回false
     */
    public boolean shouldWork(int redstonePower) {
        return switch (this) {
            case ALWAYS -> true; // 始终工作
            case REDSTONE_ON -> redstonePower > 0; // 有红石信号时工作
            case REDSTONE_OFF -> redstonePower == 0; // 没有红石信号时工作
        };
    }

    /**
     * 获取下一个模式（用于循环切换）
     * 按照：总是工作 -> 无红石信号工作 -> 有红石信号工作 -> 总是工作
     *
     * @return 下一个红石模式
     */
    public RedstoneMode getNext() {
        return switch (this) {
            case ALWAYS -> REDSTONE_OFF;
            case REDSTONE_OFF -> REDSTONE_ON;
            case REDSTONE_ON -> ALWAYS;
        };
    }

    /**
     * 获取模式的简短标识符（用于UI显示）
     *
     * @return 简短标识符
     */
    public String getShortName() {
        return switch (this) {
            case ALWAYS -> "always";
            case REDSTONE_OFF -> "no_signal";
            case REDSTONE_ON -> "with_signal";
        };
    }

    /**
     * 获取本地化的显示名称
     * @return 本地化组件
     */
    public net.minecraft.network.chat.Component getLocalizedName() {
        return net.minecraft.network.chat.Component.translatable(this.displayName);
    }

    /**
     * 从字符串获取红石模式
     * 
     * @param name 模式名称
     * @return 对应的红石模式，如果不存在则返回ALWAYS
     */
    public static RedstoneMode fromString(String name) {
        for (RedstoneMode mode : values()) {
            if (mode.getSerializedName().equals(name)) {
                return mode;
            }
        }
        return ALWAYS; // 默认模式
    }
}
