package com.example.examplemod.blockentity;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 流体储罐方块实体
 * 实现NeoForge的流体处理能力
 */
public class FluidTankBlockEntity extends BlockEntity {
    
    // 储罐容量 (16桶 = 16000 mB)
    public static final int CAPACITY = 16000;
    
    // 流体储罐
    private final FluidTank fluidTank;
    
    // 上次的流体量，用于检测变化
    private int lastFluidAmount = 0;
    
    public FluidTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ExampleMod.FLUID_TANK_BLOCK_ENTITY.get(), pos, blockState);
        
        // 创建流体储罐
        this.fluidTank = new FluidTank(CAPACITY) {
            @Override
            protected void onContentsChanged() {
                // 当流体内容改变时，标记需要保存并同步到客户端
                FluidTankBlockEntity.this.setChanged();
                FluidTankBlockEntity.this.syncToClient();
            }
        };
    }
    
    /**
     * 服务器端Tick
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity blockEntity) {
        // 检查流体量是否发生变化
        if (blockEntity.lastFluidAmount != blockEntity.fluidTank.getFluidAmount()) {
            blockEntity.lastFluidAmount = blockEntity.fluidTank.getFluidAmount();
            blockEntity.syncToClient();
        }
    }
    
    /**
     * 获取流体处理器
     */
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return fluidTank;
    }
    
    /**
     * 获取流体堆栈
     */
    public FluidStack getFluidStack() {
        return fluidTank.getFluid();
    }
    
    /**
     * 获取当前流体量
     */
    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }
    
    /**
     * 获取容量
     */
    public int getCapacity() {
        return fluidTank.getCapacity();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return fluidTank.isEmpty();
    }
    
    /**
     * 检查是否已满
     */
    public boolean isFull() {
        return fluidTank.getFluidAmount() >= fluidTank.getCapacity();
    }
    
    /**
     * 获取填充百分比 (0.0 - 1.0)
     */
    public float getFillPercentage() {
        if (fluidTank.getCapacity() == 0) {
            return 0.0f;
        }
        return (float) fluidTank.getFluidAmount() / (float) fluidTank.getCapacity();
    }
    
    /**
     * 尝试填充流体
     */
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        int filled = fluidTank.fill(resource, action);
        if (filled > 0 && action.execute()) {
            setChanged();
            syncToClient();
        }
        return filled;
    }
    
    /**
     * 尝试抽取流体
     */
    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
        FluidStack drained = fluidTank.drain(resource, action);
        if (!drained.isEmpty() && action.execute()) {
            setChanged();
            syncToClient();
        }
        return drained;
    }
    
    /**
     * 尝试抽取指定数量的流体
     */
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        FluidStack drained = fluidTank.drain(maxDrain, action);
        if (!drained.isEmpty() && action.execute()) {
            setChanged();
            syncToClient();
        }
        return drained;
    }
    
    /**
     * 同步到客户端
     */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // 使用NeoForge的新API保存FluidTank
        FluidStack fluid = fluidTank.getFluid();
        if (!fluid.isEmpty()) {
            // 保存流体堆栈
            output.store("FluidTank", FluidStack.CODEC, fluid);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // 使用NeoForge的新API加载FluidTank
        input.read("FluidTank", FluidStack.CODEC).ifPresent(fluidStack -> {
            fluidTank.setFluid(fluidStack);
        });
    }
    
    @Override
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 为客户端同步添加流体数据
        FluidStack fluid = fluidTank.getFluid();
        if (!fluid.isEmpty()) {
            // 简化的序列化，只保存基本信息
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.putString("FluidName", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            fluidTag.putInt("Amount", fluid.getAmount());
            tag.put("FluidTank", fluidTag);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        // 简化的客户端同步处理
        input.read("FluidTank", CompoundTag.CODEC).ifPresent(fluidTag -> {
            try {
                // 使用Optional API安全获取数据
                fluidTag.getString("FluidName").ifPresent(fluidName -> {
                    fluidTag.getInt("Amount").ifPresent(amount -> {
                        if (amount > 0) {
                            try {
                                ResourceLocation fluidLocation = ResourceLocation.parse(fluidName);
                                // BuiltInRegistries.FLUID.get() 也返回Optional
                                BuiltInRegistries.FLUID.get(fluidLocation).ifPresent(fluid -> {
                                    FluidStack fluidStack = new FluidStack(fluid, amount);
                                    fluidTank.setFluid(fluidStack);
                                });
                            } catch (Exception e) {
                                System.err.println("FluidTankBlockEntity: Error parsing fluid: " + e.getMessage());
                            }
                        }
                    });
                });
            } catch (Exception e) {
                System.err.println("FluidTankBlockEntity: Error loading client sync fluid data: " + e.getMessage());
            }
        });
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
