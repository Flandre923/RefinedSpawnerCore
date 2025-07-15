package com.example.examplemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;
import java.util.function.Consumer;

/**
 * 经验颗粒物品
 * 右键使用后会在玩家位置生成经验球
 * 按住Shift使用会消耗整个堆叠
 */
public class ExperienceGranuleItem extends Item {
    
    public ExperienceGranuleItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            // 获取经验值
            int experienceValue = getExperienceValue(itemStack);
            
            if (experienceValue <= 0) {
                experienceValue = 10; // 默认经验值改为10（更合理的数值）
            }
            
            // 检查是否按住Shift
            boolean isShiftPressed = player.isShiftKeyDown();
            int itemsToConsume = isShiftPressed ? itemStack.getCount() : 1;
            
            // 直接给玩家经验，而不是生成经验球（参考Mob Grinding Utils的做法）
            int totalExperience = experienceValue * itemsToConsume;
            player.giveExperiencePoints(totalExperience);
            
            // 播放音效（参考Mob Grinding Utils的音效设置）
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                0.5f, 0.8f + level.random.nextFloat() * 0.4f);
            
            // 消耗物品
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(itemsToConsume);
            }
            
            // 发送消息给玩家
            if (isShiftPressed && itemsToConsume > 1) {
                ((ServerPlayer)player).sendSystemMessage(Component.literal("Used " + itemsToConsume + " Experience Granules, gained " + totalExperience + " experience"));
            } else {
                ((ServerPlayer)player).sendSystemMessage((Component.literal("Used Experience Granule, gained " + experienceValue + " experience")));
            }
        }
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * 获取经验颗粒存储的经验值
     */
    public static int getExperienceValue(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                return tag.getInt("ExperienceValue").orElse(0);
            }
        }
        return 10; // 默认经验值改为10（更合理的数值）
    }
    
    /**
     * 设置经验颗粒存储的经验值
     */
    public static ItemStack setExperienceValue(ItemStack stack, int experienceValue) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ExperienceValue", experienceValue);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
    
    /**
     * 创建带有指定经验值的经验颗粒
     */
    public static ItemStack createWithExperience(int experienceValue) {
        ItemStack stack = new ItemStack(com.example.examplemod.ExampleMod.EXPERIENCE_GRANULE.get());
        return setExperienceValue(stack, experienceValue);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);

        int experienceValue = getExperienceValue(stack);
        tooltipAdder.accept(Component.literal("Experience: " + experienceValue + " points"));
        tooltipAdder.accept(Component.literal("Right-click to gain experience"));
        tooltipAdder.accept(Component.literal("Shift + Right-click to use entire stack"));
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // 让经验颗粒有附魔光效
        return true;
    }


}
