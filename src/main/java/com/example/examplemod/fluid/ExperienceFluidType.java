package com.example.examplemod.fluid;

import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 经验流体类型 - 用于存储击杀怪物获得的经验
 */
public class ExperienceFluidType extends FluidType {

    public ExperienceFluidType() {
        super(FluidType.Properties.create()
                .lightLevel(10) // 经验流体发光等级较高
                .density(800) // 比水稍轻，会向上浮动
                .viscosity(1200) // 较高粘度，流动较慢
                .temperature(300) // 温暖的流体
                .sound(SoundActions.BUCKET_FILL, SoundEvents.EXPERIENCE_ORB_PICKUP) // 使用经验球音效
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.EXPERIENCE_ORB_PICKUP)
                .canConvertToSource(true) // 可以形成无限源
                .canDrown(false) // 不会溺水（经验对玩家有益）
                .canExtinguish(false) // 不能灭火
                .canHydrate(false) // 不能水合
                .canPushEntity(true) // 可以推动实体
                .canSwim(true) // 可以游泳
                .supportsBoating(false) // 不支持船只（太粘稠）
        );
    }

    // 客户端扩展将在客户端设置事件中注册
}
