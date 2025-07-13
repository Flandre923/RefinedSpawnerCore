package com.example.examplemod.util;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * 经验流体辅助工具类
 * 用于处理经验到流体的转换和存储
 */
public class ExperienceFluidHelper {
    
    // 经验值到流体的转换比例 (1 经验点 = 10 mB 经验流体)
    public static final int EXP_TO_FLUID_RATIO = 10;
    
    // 一个源方块的流体量 (1000 mB)
    public static final int FLUID_BLOCK_AMOUNT = 1000;
    
    /**
     * 计算实体死亡时应该产生的经验值
     */
    public static int getExperienceFromEntity(LivingEntity entity) {
        // 使用原版的经验计算逻辑
        if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            // 大部分怪物给予 5 经验
            if (entity instanceof net.minecraft.world.entity.monster.Zombie ||
                entity instanceof net.minecraft.world.entity.monster.Skeleton ||
                entity instanceof net.minecraft.world.entity.monster.Creeper) {
                return 5;
            }
            // 末影人给予更多经验
            else if (entity instanceof net.minecraft.world.entity.monster.EnderMan) {
                return 5;
            }
            // 凋零骷髅给予更多经验
            else if (entity instanceof net.minecraft.world.entity.monster.WitherSkeleton) {
                return 5;
            }
            // 其他怪物的默认经验
            else {
                return 5;
            }
        }
        // 动物通常给予 1-3 经验
        else if (entity instanceof net.minecraft.world.entity.animal.Animal) {
            return entity.level().random.nextInt(3) + 1;
        }
        
        return 0;
    }
    
    /**
     * 将经验值转换为经验流体量 (mB)
     */
    public static int experienceToFluidAmount(int experience) {
        return experience * EXP_TO_FLUID_RATIO;
    }
    
    /**
     * 将经验流体量转换为经验值
     */
    public static int fluidAmountToExperience(int fluidAmount) {
        return fluidAmount / EXP_TO_FLUID_RATIO;
    }
    
    /**
     * 创建经验流体堆栈
     */
    public static FluidStack createExperienceFluidStack(int experience) {
        int fluidAmount = experienceToFluidAmount(experience);
        return new FluidStack(ExampleMod.EXPERIENCE.get(), fluidAmount);
    }
    
    /**
     * 尝试将经验流体存储到邻近的流体容器中
     * 优先推送到流体储罐，然后是其他流体容器
     */
    public static boolean storeExperienceFluid(ServerLevel level, BlockPos spawnerPos, int experience) {
        if (experience <= 0) {
            return false;
        }

        FluidStack experienceFluid = createExperienceFluidStack(experience);
        FluidStack originalFluid = experienceFluid.copy();

        // 搜索周围的流体容器，按优先级排序
        List<IFluidHandler> fluidHandlers = findNearbyFluidHandlers(level, spawnerPos);

        // 按优先级处理流体容器
        for (IFluidHandler handler : fluidHandlers) {
            if (experienceFluid.isEmpty()) {
                break;
            }

            // 先模拟填充，检查是否可以接受这种流体
            int simulatedFill = handler.fill(experienceFluid, IFluidHandler.FluidAction.SIMULATE);
            if (simulatedFill > 0) {
                // 实际填充
                int actualFill = handler.fill(experienceFluid, IFluidHandler.FluidAction.EXECUTE);
                if (actualFill > 0) {
                    experienceFluid.shrink(actualFill);
                    System.out.println("ExperienceFluidHelper: Stored " + actualFill + " mB of experience fluid to container");
                }
            }
        }

        // 如果还有剩余流体，尝试在世界中放置经验流体方块
        if (!experienceFluid.isEmpty()) {
            boolean placed = placeExperienceFluidInWorld(level, spawnerPos, experienceFluid.getAmount());
            if (placed) {
                System.out.println("ExperienceFluidHelper: Placed " + experienceFluid.getAmount() + " mB as fluid blocks in world");
            }
        }

        // 计算存储成功的比例
        int storedAmount = originalFluid.getAmount() - experienceFluid.getAmount();
        System.out.println("ExperienceFluidHelper: Successfully stored " + storedAmount + "/" + originalFluid.getAmount() + " mB of experience fluid");

        return storedAmount > 0; // 只要存储了一部分就算成功
    }
    
    /**
     * 查找邻近的流体处理器，按优先级排序
     * 优先级：流体储罐 > 其他流体容器
     */
    private static List<IFluidHandler> findNearbyFluidHandlers(ServerLevel level, BlockPos spawnerPos) {
        List<IFluidHandler> priorityHandlers = new java.util.ArrayList<>(); // 高优先级（流体储罐）
        List<IFluidHandler> normalHandlers = new java.util.ArrayList<>();   // 普通优先级

        int searchRange = 5; // 搜索范围

        for (int x = -searchRange; x <= searchRange; x++) {
            for (int y = -searchRange; y <= searchRange; y++) {
                for (int z = -searchRange; z <= searchRange; z++) {
                    BlockPos checkPos = spawnerPos.offset(x, y, z);

                    // 检查是否有FluidHandler capability
                    IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, checkPos, null);
                    if (fluidHandler != null) {
                        // 检查是否是我们的流体储罐
                        if (level.getBlockEntity(checkPos) instanceof com.example.examplemod.blockentity.FluidTankBlockEntity) {
                            priorityHandlers.add(fluidHandler);
                            System.out.println("ExperienceFluidHelper: Found fluid tank at " + checkPos);
                        } else {
                            normalHandlers.add(fluidHandler);
                            System.out.println("ExperienceFluidHelper: Found fluid container at " + checkPos);
                        }
                    }
                }
            }
        }

        // 合并列表，优先级高的在前面
        List<IFluidHandler> allHandlers = new java.util.ArrayList<>();
        allHandlers.addAll(priorityHandlers);
        allHandlers.addAll(normalHandlers);

        System.out.println("ExperienceFluidHelper: Found " + priorityHandlers.size() + " fluid tanks and " +
            normalHandlers.size() + " other fluid containers");

        return allHandlers;
    }
    
    /**
     * 在世界中放置经验流体方块
     */
    private static boolean placeExperienceFluidInWorld(ServerLevel level, BlockPos spawnerPos, int fluidAmount) {
        // 计算需要放置多少个源方块
        int sourceBlocks = fluidAmount / FLUID_BLOCK_AMOUNT;
        
        if (sourceBlocks <= 0) {
            return false;
        }
        
        // 在刷怪器周围寻找合适的位置放置流体
        for (int i = 0; i < sourceBlocks && i < 10; i++) { // 最多放置10个方块
            BlockPos placePos = findSuitableFluidPlacement(level, spawnerPos, i);
            if (placePos != null) {
                // 放置经验流体源方块
                FluidState experienceFluidState = ExampleMod.EXPERIENCE.get().getSource(false);
                level.setBlock(placePos, experienceFluidState.createLegacyBlock(), 3);
                System.out.println("ExperienceFluidHelper: Placed experience fluid block at " + placePos);
            }
        }
        
        return true;
    }
    
    /**
     * 寻找合适的流体放置位置
     */
    private static BlockPos findSuitableFluidPlacement(ServerLevel level, BlockPos spawnerPos, int attempt) {
        // 在刷怪器周围螺旋式搜索合适的位置
        int radius = 1 + attempt;
        
        for (int y = 0; y <= 2; y++) { // 向上搜索
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = spawnerPos.offset(x, y, z);
                    
                    if (level.getBlockState(checkPos).isAir() || 
                        level.getBlockState(checkPos).canBeReplaced()) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从经验球创建经验流体
     */
    public static FluidStack createFluidFromExperienceOrb(ExperienceOrb orb) {
        int experience = orb.getValue();
        return createExperienceFluidStack(experience);
    }
    
    /**
     * 检查流体堆栈是否是经验流体
     */
    public static boolean isExperienceFluid(FluidStack fluidStack) {
        return fluidStack.getFluid() == ExampleMod.EXPERIENCE.get();
    }
    
    /**
     * 从经验流体堆栈获取经验值
     */
    public static int getExperienceFromFluidStack(FluidStack fluidStack) {
        if (isExperienceFluid(fluidStack)) {
            return fluidAmountToExperience(fluidStack.getAmount());
        }
        return 0;
    }
}
