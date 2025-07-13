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

        // 将经验值转换为标准化的经验颗粒
        var experienceGranules = createStandardizedExperienceGranules(finalExperience);

        // 为每种规格的经验颗粒创建掉落物
        for (var granuleEntry : experienceGranules.entrySet()) {
            int experienceValue = granuleEntry.getKey();
            int count = granuleEntry.getValue();

            if (count > 0) {
                ItemStack experienceGranule = ExperienceGranuleItem.createWithExperience(experienceValue);
                experienceGranule.setCount(count);

                ItemEntity itemEntity = new ItemEntity(level,
                    entity.getX(),
                    entity.getY() + 0.5,
                    entity.getZ(),
                    experienceGranule);

                event.getDrops().add(itemEntity);

                System.out.println("ExperienceGranuleDropEvent: Added " + count + "x experience granule with " + experienceValue + " experience each");
            }
        }

        System.out.println("ExperienceGranuleDropEvent: Total experience " + finalExperience + " converted to standardized granules (base: " + baseExperience + ", looting: " + lootingLevel + ")");
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

    /**
     * 将经验值转换为标准化的经验颗粒
     * 使用固定的规格：16、8、5、3
     * 优先生成大的，余数转为小的，不够3的丢弃
     */
    private java.util.Map<Integer, Integer> createStandardizedExperienceGranules(int totalExperience) {
        java.util.Map<Integer, Integer> granules = new java.util.LinkedHashMap<>();

        // 初始化所有规格的数量为0
        granules.put(16, 0);
        granules.put(8, 0);
        granules.put(5, 0);
        granules.put(3, 0);

        int remaining = totalExperience;

        // 优先生成16经验的颗粒
        if (remaining >= 16) {
            int count16 = remaining / 16;
            granules.put(16, count16);
            remaining = remaining % 16;
        }

        // 然后生成8经验的颗粒
        if (remaining >= 8) {
            int count8 = remaining / 8;
            granules.put(8, count8);
            remaining = remaining % 8;
        }

        // 然后生成5经验的颗粒
        if (remaining >= 5) {
            int count5 = remaining / 5;
            granules.put(5, count5);
            remaining = remaining % 5;
        }

        // 最后生成3经验的颗粒
        if (remaining >= 3) {
            int count3 = remaining / 3;
            granules.put(3, count3);
            remaining = remaining % 3;
        }

        // 不够3的经验值丢弃（按照需求）
        if (remaining > 0) {
            System.out.println("ExperienceGranuleDropEvent: Discarded " + remaining + " experience (less than 3)");
        }

        // 调试输出转换结果
        StringBuilder result = new StringBuilder("ExperienceGranuleDropEvent: " + totalExperience + " exp -> ");
        for (var entry : granules.entrySet()) {
            if (entry.getValue() > 0) {
                result.append(entry.getValue()).append("x").append(entry.getKey()).append(" ");
            }
        }
        System.out.println(result.toString());

        return granules;
    }
}
