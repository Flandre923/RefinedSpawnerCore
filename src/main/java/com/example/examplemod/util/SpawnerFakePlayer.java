package com.example.examplemod.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

/**
 * 用于刷怪器模拟升级的假玩家
 * 持有带有抢夺和斩首附魔的假武器来触发原版掠夺机制
 */
public class SpawnerFakePlayer extends FakePlayer {
    
    private static final GameProfile SPAWNER_PROFILE = new GameProfile(
        UUID.fromString("12345678-1234-1234-1234-123456789012"), 
        "[MobSpawner]"
    );
    
    private final BlockPos spawnerPos;
    private int beheadingLevel = 0; // 存储斩首等级

    public SpawnerFakePlayer(ServerLevel level, BlockPos spawnerPos) {
        super(level, SPAWNER_PROFILE);
        this.spawnerPos = spawnerPos;

        // 设置假玩家位置为刷怪器位置
        this.setPos(spawnerPos.getX() + 0.5, spawnerPos.getY() + 1, spawnerPos.getZ() + 0.5);
    }
    
    /**
     * 设置假武器的抢夺和斩首等级
     */
    public void setupFakeWeapon(int lootingLevel, int beheadingLevel) {
        // 存储斩首等级
        this.beheadingLevel = beheadingLevel;

        // 创建假武器（使用铁剑）
        ItemStack fakeWeapon = new ItemStack(Items.IRON_SWORD);

        // 添加抢夺附魔
        if (lootingLevel > 0) {
            fakeWeapon.enchant(
                this.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING),
                lootingLevel
            );
        }

        // 注意：斩首附魔需要额外的模组支持，这里暂时跳过
        // 如果有斩首附魔的注册表，可以在这里添加
        // if (beheadingLevel > 0) {
        //     fakeWeapon.enchant(beheadingEnchantment, beheadingLevel);
        // }

        // 设置主手武器
        this.setItemSlot(EquipmentSlot.MAINHAND, fakeWeapon);
    }
    
    /**
     * 获取刷怪器位置
     */
    public BlockPos getSpawnerPos() {
        return spawnerPos;
    }
    
    /**
     * 检查是否是刷怪器的假玩家
     */
    public static boolean isSpawnerFakePlayer(Object entity) {
        return entity instanceof SpawnerFakePlayer;
    }
    
    /**
     * 获取假武器上的抢夺等级
     */
    public int getLootingLevel() {
        ItemStack mainHand = this.getMainHandItem();
        if (mainHand.is(Items.IRON_SWORD)) {
            return mainHand.getEnchantmentLevel(
                this.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING)
            );
        }
        return 0;
    }
    
    /**
     * 获取斩首等级
     */
    public int getBeheadingLevel() {
        return this.beheadingLevel;
    }
}
