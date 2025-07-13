package com.example.examplemod.event;

import com.example.examplemod.util.SpawnerFakePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Collection;

/**
 * 处理实体头颅掉落事件
 * 监听LivingDropsEvent来实现斩首升级效果
 */
public class EntityHeadDropEvent {
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropEvent(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide)
            return;
        // 移除健康检查，因为我们的模拟击杀可能在实体血量为0时触发
        // if (event.getEntity().getHealth() > 0.0F)
        //     return;
            
        // 检查是否是我们的FakePlayer击杀
        if (event.getSource().getEntity() instanceof SpawnerFakePlayer fakePlayer) {
            ItemStack mainHand = fakePlayer.getMainHandItem();

            System.out.println("EntityHeadDropEvent: FakePlayer killed " + event.getEntity().getType().getDescriptionId());

            // 检查是否持有铁剑（我们的假武器）
            if (mainHand.is(Items.IRON_SWORD)) {
                // 获取斩首等级（这里需要根据实际的斩首附魔实现）
                int beheadingLevel = getBeheadingLevel(fakePlayer);

                System.out.println("EntityHeadDropEvent: Beheading level: " + beheadingLevel);

                if (beheadingLevel > 0) {
                    // 计算掉落概率：每级斩首增加10%概率
                    int dropChance = event.getEntity().level().random.nextInt(10);
                    System.out.println("EntityHeadDropEvent: Drop chance: " + dropChance + " (need < " + beheadingLevel + ")");

                    if (dropChance < beheadingLevel) {
                        ItemStack headStack = getHeadFromEntity(event.getEntity());
                        System.out.println("EntityHeadDropEvent: Head stack: " + headStack);

                        if (!headStack.isEmpty()) {
                            addDrop(headStack, event.getEntity(), event.getDrops());
                            System.out.println("EntityHeadDropEvent: Successfully added head drop for " +
                                event.getEntity().getType().getDescriptionId() +
                                " with beheading level " + beheadingLevel);
                        } else {
                            System.out.println("EntityHeadDropEvent: No head available for " +
                                event.getEntity().getType().getDescriptionId());
                        }
                    } else {
                        System.out.println("EntityHeadDropEvent: Failed drop chance for " +
                            event.getEntity().getType().getDescriptionId());
                    }
                } else {
                    System.out.println("EntityHeadDropEvent: No beheading level for " +
                        event.getEntity().getType().getDescriptionId());
                }
            } else {
                System.out.println("EntityHeadDropEvent: FakePlayer not holding iron sword");
            }
        } else {
            // 添加调试信息来检查攻击者类型
            if (event.getSource().getEntity() != null) {
                System.out.println("EntityHeadDropEvent: Non-FakePlayer kill by " +
                    event.getSource().getEntity().getClass().getSimpleName());
            }
        }
    }
    
    /**
     * 获取斩首等级（暂时从SpawnerFakePlayer获取，未来可以从附魔获取）
     */
    private int getBeheadingLevel(SpawnerFakePlayer fakePlayer) {
        // 这里暂时从FakePlayer的spawner获取斩首等级
        // 未来可以改为从武器的附魔获取
        return fakePlayer.getBeheadingLevel();
    }
    
    /**
     * 根据实体类型获取对应的头颅（仅原版头颅）
     */
    private ItemStack getHeadFromEntity(LivingEntity entity) {
        System.out.println("EntityHeadDropEvent: Checking head for entity type: " + entity.getClass().getSimpleName());

        // 只支持原版头颅
        if (entity instanceof net.minecraft.world.entity.monster.Zombie &&
            !(entity instanceof net.minecraft.world.entity.monster.ZombieVillager)) {
            System.out.println("EntityHeadDropEvent: Returning zombie head");
            return new ItemStack(Items.ZOMBIE_HEAD);
        } else if (entity instanceof net.minecraft.world.entity.monster.Skeleton &&
                   !(entity instanceof net.minecraft.world.entity.monster.WitherSkeleton)) {
            System.out.println("EntityHeadDropEvent: Returning skeleton skull");
            return new ItemStack(Items.SKELETON_SKULL);
        } else if (entity instanceof net.minecraft.world.entity.monster.Creeper) {
            System.out.println("EntityHeadDropEvent: Returning creeper head");
            return new ItemStack(Items.CREEPER_HEAD);
        } else if (entity instanceof net.minecraft.world.entity.monster.WitherSkeleton) {
            System.out.println("EntityHeadDropEvent: Returning wither skeleton skull");
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        } else if (entity instanceof net.minecraft.world.entity.monster.EnderMan) {
            System.out.println("EntityHeadDropEvent: Returning dragon head for enderman");
            return new ItemStack(Items.DRAGON_HEAD); // 使用龙头作为末影人头颅的替代
        } else if (entity instanceof Player player) {
            System.out.println("EntityHeadDropEvent: Returning player head");
            // 玩家头颅
            ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
            // 这里可以设置玩家头颅的皮肤，但需要额外的NBT处理
            return playerHead;
        }

        System.out.println("EntityHeadDropEvent: No head available for entity type: " + entity.getClass().getSimpleName());
        return ItemStack.EMPTY;
    }
    
    /**
     * 添加掉落物到掉落列表
     */
    private void addDrop(ItemStack stack, LivingEntity entity, Collection<ItemEntity> collection) {
        if (stack.getCount() <= 0)
            return;
            
        ItemEntity entityItem = new ItemEntity(
            entity.level(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            stack
        );
        entityItem.setDefaultPickUpDelay();
        collection.add(entityItem);
    }
}
