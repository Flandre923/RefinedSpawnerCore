package com.example.examplemod.item;

import com.example.examplemod.spawner.SpawnerModuleType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

/**
 * 刷怪器模块物品
 */
public class SpawnerModuleItem extends Item {
    
    private final SpawnerModuleType moduleType;
    
    public SpawnerModuleItem(Properties properties, SpawnerModuleType moduleType) {
        super(properties);
        this.moduleType = moduleType;
    }
    
    public SpawnerModuleType getModuleType() {
        return moduleType;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder,flag);
        
        // 添加模块描述
        tooltipAdder.accept(moduleType.getDescriptionComponent());

        // 添加效果说明
        int effectValue = moduleType.getEffectValue();
        String effectText = switch (moduleType) {
            case RANGE_REDUCER -> "Range: " + effectValue;
            case RANGE_EXPANDER -> "Range: +" + effectValue;
            case MIN_DELAY_REDUCER -> "Min Delay: " + effectValue + " ticks";
            case MAX_DELAY_REDUCER -> "Max Delay: " + effectValue + " ticks";
            case COUNT_BOOSTER -> "Spawn Count: +" + effectValue;
            case PLAYER_IGNORER -> "Ignores player distance";
        };

        tooltipAdder.accept(Component.literal("§7" + effectText));
    }
}
