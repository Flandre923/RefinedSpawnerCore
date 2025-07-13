package com.example.examplemod;

import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.example.examplemod.fluid.MagicWaterFluid;
import com.example.examplemod.fluid.MagicWaterFluidType;
import com.example.examplemod.fluid.MagicWaterClientExtensions;
import com.example.examplemod.fluid.ExperienceFluid;
import com.example.examplemod.fluid.ExperienceFluidType;
import com.example.examplemod.fluid.ExperienceClientExtensions;
import com.example.examplemod.blockentity.MobSpawnerBlock;
import com.example.examplemod.blockentity.MobSpawnerBlockEntity;
import com.example.examplemod.blockentity.MobSpawnerMenu;
import com.example.examplemod.blockentity.MobSpawnerScreen;
import com.example.examplemod.block.FluidTankBlock;
import com.example.examplemod.blockentity.FluidTankBlockEntity;
import com.example.examplemod.blockentity.SpawnEggMobSpawnerMenu;
import com.example.examplemod.blockentity.SpawnEggMobSpawnerScreen;
import com.example.examplemod.client.SpawnAreaRenderer;
import com.example.examplemod.network.MobSpawnerUpdatePacket;
import com.example.examplemod.network.SpawnAreaDataPacket;
import com.example.examplemod.event.EntityHeadDropEvent;
import com.example.examplemod.network.SpawnOffsetUpdatePacket;
import com.example.examplemod.network.RedstoneModeUpdatePacket;
import com.example.examplemod.network.RedstoneModeClientSyncPacket;
import com.example.examplemod.item.SpawnerModuleItem;
import com.example.examplemod.spawner.SpawnerModuleType;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "examplemod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold Fluids which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, MODID);
    // Create a Deferred Register to hold FluidTypes which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold MenuTypes which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Magic Water Fluid Type
    public static final DeferredHolder<FluidType, FluidType> MAGIC_WATER_TYPE = FLUID_TYPES.register("magic_water", MagicWaterFluidType::new);

    // Magic Water Fluids
    public static final DeferredHolder<Fluid, FlowingFluid> MAGIC_WATER = FLUIDS.register("magic_water", () -> new MagicWaterFluid.Source());
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_MAGIC_WATER = FLUIDS.register("flowing_magic_water", () -> new MagicWaterFluid.Flowing());

    // Magic Water Block
    public static final DeferredBlock<LiquidBlock> MAGIC_WATER_BLOCK = BLOCKS.register("magic_water",
        () -> new LiquidBlock(MAGIC_WATER.get(), BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "magic_water")))
            .mapColor(MapColor.WATER).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)));

    // Magic Water Bucket
    public static final DeferredItem<BucketItem> MAGIC_WATER_BUCKET = ITEMS.register("magic_water_bucket",
        () -> new BucketItem(MAGIC_WATER.get(), new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "magic_water_bucket")))
            .craftRemainder(Items.BUCKET).stacksTo(1)));

    // Experience Fluid Type
    public static final DeferredHolder<FluidType, FluidType> EXPERIENCE_TYPE = FLUID_TYPES.register("experience", ExperienceFluidType::new);

    // Experience Fluids
    public static final DeferredHolder<Fluid, FlowingFluid> EXPERIENCE = FLUIDS.register("experience", () -> new ExperienceFluid.Source());
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_EXPERIENCE = FLUIDS.register("flowing_experience", () -> new ExperienceFluid.Flowing());

    // Experience Block
    public static final DeferredBlock<LiquidBlock> EXPERIENCE_BLOCK = BLOCKS.register("experience",
        () -> new LiquidBlock(EXPERIENCE.get(), BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "experience")))
            .mapColor(MapColor.COLOR_LIGHT_GREEN).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY)
            .noLootTable().liquid().sound(SoundType.EMPTY).lightLevel((state) -> 10)));

    // Experience Bucket
    public static final DeferredItem<BucketItem> EXPERIENCE_BUCKET = ITEMS.register("experience_bucket",
        () -> new BucketItem(EXPERIENCE.get(), new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "experience_bucket")))
            .craftRemainder(Items.BUCKET).stacksTo(1)));

    // Fluid Tank Block
    public static final DeferredBlock<FluidTankBlock> FLUID_TANK_BLOCK = BLOCKS.register("fluid_tank",
        () -> new FluidTankBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "fluid_tank")))
            .mapColor(MapColor.METAL).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));

    // Fluid Tank Block Item
    public static final DeferredItem<BlockItem> FLUID_TANK_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("fluid_tank", FLUID_TANK_BLOCK);

    // Fluid Tank Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("fluid_tank", () -> new BlockEntityType(
            FluidTankBlockEntity::new, FLUID_TANK_BLOCK.get()));

    // Mob Spawner Block
    public static final DeferredBlock<MobSpawnerBlock> MOB_SPAWNER_BLOCK = BLOCKS.register("mob_spawner",
        () -> new MobSpawnerBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "mob_spawner")))
            .mapColor(MapColor.STONE).strength(5.0F).requiresCorrectToolForDrops()));

    // Mob Spawner Block Item
    public static final DeferredItem<BlockItem> MOB_SPAWNER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mob_spawner", MOB_SPAWNER_BLOCK);

    // Mob Spawner Block Entity Type
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobSpawnerBlockEntity>> MOB_SPAWNER_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("mob_spawner", () -> new BlockEntityType<>(
            MobSpawnerBlockEntity::new, MOB_SPAWNER_BLOCK.get()));

    // Mob Spawner Menu Type
    public static final DeferredHolder<MenuType<?>, MenuType<MobSpawnerMenu>> MOB_SPAWNER_MENU =
        MENU_TYPES.register("mob_spawner", () -> IMenuTypeExtension.create(MobSpawnerMenu::new));

    // Spawn Egg Mob Spawner Menu Type
    public static final DeferredHolder<MenuType<?>, MenuType<SpawnEggMobSpawnerMenu>> SPAWN_EGG_MOB_SPAWNER_MENU =
        MENU_TYPES.register("spawn_egg_mob_spawner", () -> IMenuTypeExtension.create(SpawnEggMobSpawnerMenu::new));

    // Spawner Module Items
    public static final DeferredItem<SpawnerModuleItem> RANGE_REDUCER_MODULE = ITEMS.register("range_reducer_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "range_reducer_module")))
            .stacksTo(16), SpawnerModuleType.RANGE_REDUCER));

    public static final DeferredItem<SpawnerModuleItem> RANGE_EXPANDER_MODULE = ITEMS.register("range_expander_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "range_expander_module")))
            .stacksTo(16), SpawnerModuleType.RANGE_EXPANDER));

    public static final DeferredItem<SpawnerModuleItem> MIN_DELAY_REDUCER_MODULE = ITEMS.register("min_delay_reducer_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "min_delay_reducer_module")))
            .stacksTo(16), SpawnerModuleType.MIN_DELAY_REDUCER));

    public static final DeferredItem<SpawnerModuleItem> MAX_DELAY_REDUCER_MODULE = ITEMS.register("max_delay_reducer_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "max_delay_reducer_module")))
            .stacksTo(16), SpawnerModuleType.MAX_DELAY_REDUCER));

    public static final DeferredItem<SpawnerModuleItem> COUNT_BOOSTER_MODULE = ITEMS.register("count_booster_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "count_booster_module")))
            .stacksTo(16), SpawnerModuleType.COUNT_BOOSTER));

    public static final DeferredItem<SpawnerModuleItem> PLAYER_IGNORER_MODULE = ITEMS.register("player_ignorer_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "player_ignorer_module")))
            .stacksTo(16), SpawnerModuleType.PLAYER_IGNORER));

    public static final DeferredItem<SpawnerModuleItem> SIMULATION_UPGRADE_MODULE = ITEMS.register("simulation_upgrade_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "simulation_upgrade_module")))
            .stacksTo(16), SpawnerModuleType.SIMULATION_UPGRADE));

    public static final DeferredItem<SpawnerModuleItem> LOOTING_UPGRADE_MODULE = ITEMS.register("looting_upgrade_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "looting_upgrade_module")))
            .stacksTo(16), SpawnerModuleType.LOOTING_UPGRADE));

    public static final DeferredItem<SpawnerModuleItem> BEHEADING_UPGRADE_MODULE = ITEMS.register("beheading_upgrade_module",
        () -> new SpawnerModuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "beheading_upgrade_module")))
            .stacksTo(16), SpawnerModuleType.BEHEADING_UPGRADE));

    // Experience Granule Item
    public static final DeferredItem<com.example.examplemod.item.ExperienceGranuleItem> EXPERIENCE_GRANULE = ITEMS.register("experience_granule",
        () -> new com.example.examplemod.item.ExperienceGranuleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "experience_granule")))
            .stacksTo(64)));

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(MAGIC_WATER_BUCKET.get()); // Add the magic water bucket to the tab
                output.accept(MOB_SPAWNER_BLOCK_ITEM.get()); // Add the mob spawner block to the tab
                output.accept(EXPERIENCE_GRANULE.get()); // Add the experience granule to the tab

                // Add spawner modules
                output.accept(RANGE_REDUCER_MODULE.get());
                output.accept(RANGE_EXPANDER_MODULE.get());
                output.accept(MIN_DELAY_REDUCER_MODULE.get());
                output.accept(MAX_DELAY_REDUCER_MODULE.get());
                output.accept(COUNT_BOOSTER_MODULE.get());
                output.accept(PLAYER_IGNORER_MODULE.get());
                output.accept(SIMULATION_UPGRADE_MODULE.get());
                output.accept(LOOTING_UPGRADE_MODULE.get());
                output.accept(BEHEADING_UPGRADE_MODULE.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so fluids get registered
        FLUIDS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so fluid types get registered
        FLUID_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entity types get registered
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so menu types get registered
        MENU_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register event handlers
        NeoForge.EVENT_BUS.register(new EntityHeadDropEvent());
        NeoForge.EVENT_BUS.register(new com.example.examplemod.event.ExperienceGranuleDropEvent());



        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register network packets
        modEventBus.addListener(this::registerPayloads);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToServer(
            MobSpawnerUpdatePacket.TYPE,
            MobSpawnerUpdatePacket.STREAM_CODEC,
            MobSpawnerUpdatePacket::handle
        );
        registrar.playToServer(
            SpawnOffsetUpdatePacket.TYPE,
            SpawnOffsetUpdatePacket.STREAM_CODEC,
            SpawnOffsetUpdatePacket::handle
        );
        registrar.playToServer(
            RedstoneModeUpdatePacket.TYPE,
            RedstoneModeUpdatePacket.STREAM_CODEC,
            RedstoneModeUpdatePacket::handleServer
        );
        registrar.playToClient(
            RedstoneModeClientSyncPacket.TYPE,
            RedstoneModeClientSyncPacket.STREAM_CODEC,
            RedstoneModeClientSyncPacket::handleClient
        );
        registrar.playToClient(
            SpawnAreaDataPacket.TYPE,
            SpawnAreaDataPacket.STREAM_CODEC,
            SpawnAreaDataPacket::handle
        );
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAGIC_WATER_BUCKET);
            event.accept(EXPERIENCE_BUCKET);
            event.accept(MOB_SPAWNER_BLOCK_ITEM);
            event.accept(FLUID_TANK_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID,  value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            // 注册流体客户端扩展
            event.registerFluidType(new MagicWaterClientExtensions(), MAGIC_WATER_TYPE.get());
            event.registerFluidType(new ExperienceClientExtensions(), EXPERIENCE_TYPE.get());
        }

        @SubscribeEvent
        public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(MOB_SPAWNER_MENU.get(), MobSpawnerScreen::new);
            event.register(SPAWN_EGG_MOB_SPAWNER_MENU.get(), SpawnEggMobSpawnerScreen::new);
        }

        @SubscribeEvent
        public static void onClientSetup(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
            // 在客户端预先注册SpawnAreaRenderer和ClientEventHandler
            if (event.getLevel().isClientSide()) {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(SpawnAreaRenderer.class);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.example.examplemod.client.ClientEventHandler.class);
            }
        }
    }


}
