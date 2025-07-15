package com.example.examplemod.event;

import com.example.examplemod.item.ExperienceGranuleItem;
import com.example.examplemod.util.SpawnerFakePlayer;
import com.example.examplemod.util.ExperienceFluidHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 经验颗粒掉落事件处理器
 * 当模拟升级击杀生物时，生成经验颗粒物品
 */
public class ExperienceGranuleDropEvent {
    
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        // 检查是否是SpawnerFakePlayer击杀的
        if (!(event.getSource().getEntity() instanceof SpawnerFakePlayer fakePlayer)) {
            return;
        }
        
        // 获取被击杀的实体
        var entity = event.getEntity();
        Level level = entity.level();
        
        if (level.isClientSide) {
            return;
        }
        
        // 计算基础经验值
        int baseExperience = ExperienceFluidHelper.getExperienceFromEntity(entity);
        
        if (baseExperience <= 0) {
            return;
        }
        
        // 获取抢夺等级
        int lootingLevel = 0;
        ItemStack weapon = fakePlayer.getMainHandItem();
        if (!weapon.isEmpty()) {
            // 获取抢夺附魔等级
            var enchantmentRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var lootingEnchantment = enchantmentRegistry.getOrThrow(Enchantments.LOOTING);
            lootingLevel = weapon.getEnchantmentLevel(lootingEnchantment);
        }
        
        // 计算最终经验值（抢夺影响）
        int finalExperience = calculateExperienceWithLooting(baseExperience, lootingLevel, level.random);

        // 简化：只生成10经验值的经验颗粒（更合理的数值）
        int granuleCount = finalExperience / 10; // 每10经验生成1个颗粒
        int remainingExperience = finalExperience % 10; // 剩余经验丢弃

        if (granuleCount > 0) {
            ItemStack experienceGranule = ExperienceGranuleItem.createWithExperience(10);
            experienceGranule.setCount(granuleCount);

            ItemEntity itemEntity = new ItemEntity(level,
                entity.getX(),
                entity.getY() + 0.5,
                entity.getZ(),
                experienceGranule);

            event.getDrops().add(itemEntity);

            System.out.println("ExperienceGranuleDropEvent: Added " + granuleCount + "x experience granule (10 exp each) from " + finalExperience + " total experience");
        }

        if (remainingExperience > 0) {
            System.out.println("ExperienceGranuleDropEvent: Discarded " + remainingExperience + " experience (less than 10)");
        }
    }
    
    /**
     * 根据抢夺等级计算经验值
     * 抢夺附魔会增加经验掉落
     */
    private int calculateExperienceWithLooting(int baseExperience, int lootingLevel, net.minecraft.util.RandomSource random) {
        if (lootingLevel <= 0) {
            return baseExperience;
        }
        
        // 抢夺附魔对经验的影响：
        // 每级抢夺有25%的概率额外获得1点经验
        int bonusExperience = 0;
        for (int i = 0; i < lootingLevel; i++) {
            if (random.nextFloat() < 0.25f) {
                bonusExperience++;
            }
        }
        
        return baseExperience + bonusExperience;
    }

}
