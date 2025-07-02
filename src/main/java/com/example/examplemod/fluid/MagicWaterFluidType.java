package com.example.examplemod.fluid;

import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

public class MagicWaterFluidType extends FluidType {

    public MagicWaterFluidType() {
        super(FluidType.Properties.create()
                .lightLevel(2) // 发光等级
                .density(-1000) // 负密度，使流体向上流动
                .viscosity(500) // 降低粘度，使流动更快
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .canConvertToSource(false) // 不能形成无限水源
                .canDrown(true) // 可以溺水
                .canExtinguish(true) // 可以灭火
                .canHydrate(true) // 可以水合
                .canPushEntity(true) // 可以推动实体
                .canSwim(true) // 可以游泳
                .supportsBoating(true) // 支持船只
        );
    }

    // 客户端扩展将在客户端设置事件中注册
}
