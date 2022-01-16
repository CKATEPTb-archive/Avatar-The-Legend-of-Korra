package ru.ckateptb.abilityslots.common.paper;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

/**
 * Represents a collection tags to identify materials that share common properties.
 * Will map to minecraft for missing tags, as well as custom ones that may be useful.
 */
@SuppressWarnings({"NonFinalUtilityClass", "unused", "WeakerAccess"})
public class MaterialTags {

    public static final MaterialSetTag ARROWS = new MaterialSetTag(keyFor("arrows"))
            .endsWith("ARROW")
            .ensureSize("ARROWS", 3);
    /**
     * Covers all colors of beds.
     */
    public static final MaterialSetTag BEDS = new MaterialSetTag(keyFor("beds"))
            .endsWith("_BED")
            .ensureSize("BEDS", 16);
    /**
     * Covers all bucket items.
     */
    public static final MaterialSetTag BUCKETS = new MaterialSetTag(keyFor("buckets"))
            .endsWith("BUCKET")
            .ensureSize("BUCKETS", 10);
    /**
     * Covers coal and charcoal.
     */
    public static final MaterialSetTag COALS = new MaterialSetTag(keyFor("coals"))
            .add(Material.COAL, Material.CHARCOAL);
    /**
     * Covers both cobblestone wall variants.
     */
    public static final MaterialSetTag COBBLESTONE_WALLS = new MaterialSetTag(keyFor("cobblestone_walls"))
            .endsWith("COBBLESTONE_WALL")
            .ensureSize("COBBLESTONE_WALLS", 2);
    /**
     * Covers both cobblestone and mossy Cobblestone.
     */
    public static final MaterialSetTag COBBLESTONES = new MaterialSetTag(keyFor("cobblestones"))
            .add(Material.COBBLESTONE, Material.MOSSY_COBBLESTONE);
    /**
     * Covers all colors of concrete.
     */
    public static final MaterialSetTag CONCRETES = new MaterialSetTag(keyFor("concretes"))
            .endsWith("_CONCRETE")
            .ensureSize("CONCRETES", 16);
    /**
     * Covers all colors of concrete powder.
     */
    public static final MaterialSetTag CONCRETE_POWDER = new MaterialSetTag(keyFor("concrete_powder"))
            .endsWith("_CONCRETE_POWDER")
            .ensureSize("CONCRETE_POWDER", 16);
    /**
     * Covers the two types of cooked fish.
     */
    public static final MaterialSetTag COOKED_FISH = new MaterialSetTag(keyFor("cooked_fish"))
            .add(Material.COOKED_COD, Material.COOKED_SALMON);
    /**
     * Covers all variants of doors.
     */
    public static final MaterialSetTag DOORS = new MaterialSetTag(keyFor("doors"))
            .endsWith("_DOOR")
            .ensureSize("DOORS", 9);
    /**
     * Covers all dyes.
     */
    public static final MaterialSetTag DYES = new MaterialSetTag(keyFor("dyes"))
            .endsWith("_DYE")
            .ensureSize("DYES", 16);
    /**
     * Covers all variants of gates.
     */
    public static final MaterialSetTag FENCE_GATES = new MaterialSetTag(keyFor("fence_gates"))
            .endsWith("_GATE")
            .ensureSize("FENCE_GATES", 8);
    /**
     * Covers all variants of fences.
     */
    public static final MaterialSetTag FENCES = new MaterialSetTag(keyFor("fences"))
            .endsWith("_FENCE")
            .ensureSize("FENCES", 9);
    /**
     * Covers all variants of fish buckets.
     */
    public static final MaterialSetTag FISH_BUCKETS = new MaterialSetTag(keyFor("fish_buckets"))
            .add(Material.COD_BUCKET, Material.PUFFERFISH_BUCKET, Material.SALMON_BUCKET, Material.TROPICAL_FISH_BUCKET);
    /**
     * Covers the non-colored glass and 16 stained glass (not panes).
     */
    public static final MaterialSetTag GLASS = new MaterialSetTag(keyFor("glass"))
            .endsWith("_GLASS")
            .add(Material.GLASS)
            .ensureSize("GLASS", 18);
    /**
     * Covers the non-colored glass panes and stained glass panes (panes only).
     */
    public static final MaterialSetTag GLASS_PANES = new MaterialSetTag(keyFor("glass_panes"))
            .endsWith("GLASS_PANE")
            .ensureSize("GLASS_PANES", 17);
    /**
     * Covers all glazed terracotta blocks.
     */
    public static final MaterialSetTag GLAZED_TERRACOTTA = new MaterialSetTag(keyFor("glazed_terracotta"))
            .endsWith("GLAZED_TERRACOTTA")
            .ensureSize("GLAZED_TERRACOTTA", 16);
    /**
     * Covers the colors of stained terracotta.
     */
    public static final MaterialSetTag STAINED_TERRACOTTA = new MaterialSetTag(keyFor("stained_terracotta"))
            .endsWith("TERRACOTTA")
            .not(Material.TERRACOTTA)
            .notEndsWith("GLAZED_TERRACOTTA")
            .ensureSize("STAINED_TERRACOTTA", 16);
    /**
     * Covers terracotta along with the stained variants.
     */
    public static final MaterialSetTag TERRACOTTA = new MaterialSetTag(keyFor("terracotta"))
            .endsWith("TERRACOTTA")
            .ensureSize("TERRACOTTA", 33);
    /**
     * Covers both golden apples.
     */
    public static final MaterialSetTag GOLDEN_APPLES = new MaterialSetTag(keyFor("golden_apples"))
            .endsWith("GOLDEN_APPLE")
            .ensureSize("GOLDEN_APPLES", 2);
    /**
     * Covers the variants of horse armor.
     */
    public static final MaterialSetTag HORSE_ARMORS = new MaterialSetTag(keyFor("horse_armors"))
            .endsWith("_HORSE_ARMOR")
            .ensureSize("HORSE_ARMORS", 4);
    /**
     * Covers the variants of infested blocks.
     */
    public static final MaterialSetTag INFESTED_BLOCKS = new MaterialSetTag(keyFor("infested_blocks"))
            .startsWith("INFESTED_")
            .ensureSize("INFESTED_BLOCKS", 7);
    /**
     * Covers the variants of mushroom blocks.
     */
    public static final MaterialSetTag MUSHROOM_BLOCKS = new MaterialSetTag(keyFor("mushroom_blocks"))
            .endsWith("MUSHROOM_BLOCK")
            .add(Material.MUSHROOM_STEM)
            .ensureSize("MUSHROOM_BLOCKS", 3);
    /**
     * Covers all mushrooms.
     */
    public static final MaterialSetTag MUSHROOMS = new MaterialSetTag(keyFor("mushrooms"))
            .add(Material.BROWN_MUSHROOM, Material.RED_MUSHROOM);
    /**
     * Covers all music disc items.
     */
    public static final MaterialSetTag MUSIC_DISCS = new MaterialSetTag(keyFor("music_discs"))
            .startsWith("MUSIC_DISC_");
    /**
     * Covers all ores.
     */
    public static final MaterialSetTag ORES = new MaterialSetTag(keyFor("ores"))
            .add(Material.ANCIENT_DEBRIS)
            .endsWith("_ORE")
            .ensureSize("ORES", 19);
    /**
     * Covers all piston typed items and blocks including the piston head and moving piston.
     */
    public static final MaterialSetTag PISTONS = new MaterialSetTag(keyFor("pistons"))
            .contains("PISTON")
            .ensureSize("PISTONS", 4);
    /**
     * Covers all potato items.
     */
    public static final MaterialSetTag POTATOES = new MaterialSetTag(keyFor("potatoes"))
            .endsWith("POTATO")
            .ensureSize("POTATOES", 3);
    /**
     * Covers all wooden pressure plates and the weighted pressure plates and the stone pressure plate.
     */
    public static final MaterialSetTag PRESSURE_PLATES = new MaterialSetTag(keyFor("pressure_plates"))
            .endsWith("_PRESSURE_PLATE")
            .ensureSize("PRESSURE_PLATES", 12);
    /**
     * Covers the variants of prismarine blocks.
     */
    public static final MaterialSetTag PRISMARINE = new MaterialSetTag(keyFor("prismarine"))
            .add(Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE);
    /**
     * Covers the variants of prismarine slabs.
     */
    public static final MaterialSetTag PRISMARINE_SLABS = new MaterialSetTag(keyFor("prismarine_slabs"))
            .add(Material.PRISMARINE_SLAB, Material.PRISMARINE_BRICK_SLAB, Material.DARK_PRISMARINE_SLAB);
    /**
     * Covers the variants of prismarine stairs.
     */
    public static final MaterialSetTag PRISMARINE_STAIRS = new MaterialSetTag(keyFor("prismarine_stairs"))
            .add(Material.PRISMARINE_STAIRS, Material.PRISMARINE_BRICK_STAIRS, Material.DARK_PRISMARINE_STAIRS);
    /**
     * Covers the variants of pumpkins.
     */
    public static final MaterialSetTag PUMPKINS = new MaterialSetTag(keyFor("pumpkins"))
            .add(Material.CARVED_PUMPKIN, Material.JACK_O_LANTERN, Material.PUMPKIN);
    /**
     * Covers the variants of quartz blocks.
     */
    public static final MaterialSetTag QUARTZ_BLOCKS = new MaterialSetTag(keyFor("quartz_blocks"))
            .add(Material.QUARTZ_BLOCK, Material.QUARTZ_PILLAR, Material.CHISELED_QUARTZ_BLOCK, Material.SMOOTH_QUARTZ);
    /**
     * Covers all uncooked fish items.
     */
    public static final MaterialSetTag RAW_FISH = new MaterialSetTag(keyFor("raw_fish"))
            .add(Material.COD, Material.PUFFERFISH, Material.SALMON, Material.TROPICAL_FISH);
    /**
     * Covers the variants of red sandstone blocks.
     */
    public static final MaterialSetTag RED_SANDSTONES = new MaterialSetTag(keyFor("red_sandstones"))
            .endsWith("RED_SANDSTONE")
            .ensureSize("RED_SANDSTONES", 4);
    /**
     * Covers the variants of sandstone blocks.
     */
    public static final MaterialSetTag SANDSTONES = new MaterialSetTag(keyFor("sandstones"))
            .add(Material.SANDSTONE, Material.CHISELED_SANDSTONE, Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE);
    /**
     * Covers sponge and wet sponge.
     */
    public static final MaterialSetTag SPONGES = new MaterialSetTag(keyFor("sponges"))
            .endsWith("SPONGE")
            .ensureSize("SPONGES", 2);
    /**
     * Covers the non-colored and colored shulker boxes.
     */
    public static final MaterialSetTag SHULKER_BOXES = new MaterialSetTag(keyFor("shulker_boxes"))
            .endsWith("SHULKER_BOX")
            .ensureSize("SHULKER_BOXES", 17);
    /**
     * Covers zombie, creeper, skeleton, dragon, and player heads.
     */
    public static final MaterialSetTag SKULLS = new MaterialSetTag(keyFor("skulls"))
            .endsWith("_HEAD")
            .endsWith("_SKULL")
            .not(Material.PISTON_HEAD)
            .ensureSize("SKULLS", 12);
    /**
     * Covers all spawn egg items.
     */
    public static final MaterialSetTag SPAWN_EGGS = new MaterialSetTag(keyFor("spawn_eggs"))
            .endsWith("_SPAWN_EGG")
            .ensureSize("SPAWN_EGGS", 67);
    /**
     * Covers all colors of stained glass.
     */
    public static final MaterialSetTag STAINED_GLASS = new MaterialSetTag(keyFor("stained_glass"))
            .endsWith("_STAINED_GLASS")
            .ensureSize("STAINED_GLASS", 16);
    /**
     * Covers all colors of stained glass panes.
     */
    public static final MaterialSetTag STAINED_GLASS_PANES = new MaterialSetTag(keyFor("stained_glass_panes"))
            .endsWith("STAINED_GLASS_PANE")
            .ensureSize("STAINED_GLASS_PANES", 16);
    /**
     * Covers all variants of trapdoors.
     */
    public static final MaterialSetTag TRAPDOORS = new MaterialSetTag(keyFor("trapdoors"))
            .endsWith("_TRAPDOOR")
            .ensureSize("TRAPDOORS", 9);
    /**
     * Covers all wood variants of doors.
     */
    public static final MaterialSetTag WOODEN_DOORS = new MaterialSetTag(keyFor("wooden_doors"))
            .endsWith("_DOOR")
            .not(Material.IRON_DOOR)
            .ensureSize("WOODEN_DOORS", 8);
    /**
     * Covers all wood variants of fences.
     */
    public static final MaterialSetTag WOODEN_FENCES = new MaterialSetTag(keyFor("wooden_fences"))
            .endsWith("_FENCE")
            .not(Material.NETHER_BRICK_FENCE)
            .ensureSize("WOODEN_FENCES", 8);
    /**
     * Covers all wood variants of trapdoors.
     */
    public static final MaterialSetTag WOODEN_TRAPDOORS = new MaterialSetTag(keyFor("wooden_trapdoors"))
            .endsWith("_TRAPDOOR")
            .not(Material.IRON_TRAPDOOR)
            .ensureSize("WOODEN_TRAPDOORS", 8);
    /**
     * Covers the wood variants of gates.
     */
    public static final MaterialSetTag WOODEN_GATES = new MaterialSetTag(keyFor("wooden_gates"))
            .endsWith("_GATE")
            .ensureSize("WOODEN_GATES", 8);
    /**
     * Covers the variants of purpur.
     */
    public static final MaterialSetTag PURPUR = new MaterialSetTag(keyFor("purpur"))
            .startsWith("PURPUR_")
            .ensureSize("PURPUR", 4);
    /**
     * Covers the variants of signs.
     */
    public static final MaterialSetTag SIGNS = new MaterialSetTag(keyFor("signs"))
            .endsWith("_SIGN")
            .ensureSize("SIGNS", 16);
    /**
     * Covers the variants of a regular torch.
     */
    public static final MaterialSetTag TORCH = new MaterialSetTag(keyFor("torch"))
            .add(Material.TORCH, Material.WALL_TORCH)
            .ensureSize("TORCH", 2);
    /**
     * Covers the variants of a redstone torch.
     */
    public static final MaterialSetTag REDSTONE_TORCH = new MaterialSetTag(keyFor("restone_torch"))
            .add(Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH)
            .ensureSize("REDSTONE_TORCH", 2);
    /**
     * Covers the variants of a soul torch.
     */
    public static final MaterialSetTag SOUL_TORCH = new MaterialSetTag(keyFor("soul_torch"))
            .add(Material.SOUL_TORCH, Material.SOUL_WALL_TORCH)
            .ensureSize("SOUL_TORCH", 2);
    /**
     * Covers the variants of torches.
     */
    public static final MaterialSetTag TORCHES = new MaterialSetTag(keyFor("torches"))
            .add(TORCH, REDSTONE_TORCH, SOUL_TORCH)
            .ensureSize("TORCHES", 6);
    /**
     * Covers the variants of lanterns.
     */
    public static final MaterialSetTag LANTERNS = new MaterialSetTag(keyFor("lanterns"))
            .add(Material.LANTERN, Material.SOUL_LANTERN)
            .ensureSize("LANTERNS", 2);
    /**
     * Covers the variants of rails.
     */
    public static final MaterialSetTag RAILS = new MaterialSetTag(keyFor("rails"))
            .endsWith("RAIL")
            .ensureSize("RAILS", 4);
    /**
     * Covers the variants of swords.
     */
    public static final MaterialSetTag SWORDS = new MaterialSetTag(keyFor("swords"))
            .endsWith("_SWORD")
            .ensureSize("SWORDS", 6);
    /**
     * Covers the variants of shovels.
     */
    public static final MaterialSetTag SHOVELS = new MaterialSetTag(keyFor("shovels"))
            .endsWith("_SHOVEL")
            .ensureSize("SHOVELS", 6);
    /**
     * Covers the variants of pickaxes.
     */
    public static final MaterialSetTag PICKAXES = new MaterialSetTag(keyFor("pickaxes"))
            .endsWith("_PICKAXE")
            .ensureSize("PICKAXES", 6);
    /**
     * Covers the variants of axes.
     */
    public static final MaterialSetTag AXES = new MaterialSetTag(keyFor("axes"))
            .endsWith("_AXE")
            .ensureSize("AXES", 6);
    /**
     * Covers the variants of hoes.
     */
    public static final MaterialSetTag HOES = new MaterialSetTag(keyFor("hoes"))
            .endsWith("_HOE")
            .ensureSize("HOES", 6);
    /**
     * Covers the variants of helmets.
     */
    public static final MaterialSetTag HELMETS = new MaterialSetTag(keyFor("helmets"))
            .endsWith("_HELMET")
            .ensureSize("HELMETS", 7);
    /**
     * Covers the variants of items that can be equipped in the helmet slot.
     */
    public static final MaterialSetTag HEAD_EQUIPPABLE = new MaterialSetTag(keyFor("head_equippable"))
            .endsWith("_HELMET")
            .add(SKULLS)
            .add(Material.CARVED_PUMPKIN)
            .ensureSize("HEAD_EQUIPPABLE", 20);
    /**
     * Covers the variants of chestplate.
     */
    public static final MaterialSetTag CHESTPLATES = new MaterialSetTag(keyFor("chestplates"))
            .endsWith("_CHESTPLATE")
            .ensureSize("CHESTPLATES", 6);
    /**
     * Covers the variants of items that can be equipped in the chest slot.
     */
    public static final MaterialSetTag CHEST_EQUIPPABLE = new MaterialSetTag(keyFor("chest_equippable"))
            .endsWith("_CHESTPLATE")
            .add(Material.ELYTRA)
            .ensureSize("CHEST_EQUIPPABLE", 7);
    /**
     * Covers the variants of leggings.
     */
    public static final MaterialSetTag LEGGINGS = new MaterialSetTag(keyFor("leggings"))
            .endsWith("_LEGGINGS")
            .ensureSize("LEGGINGS", 6);
    /**
     * Covers the variants of boots.
     */
    public static final MaterialSetTag BOOTS = new MaterialSetTag(keyFor("boots"))
            .endsWith("_BOOTS")
            .ensureSize("BOOTS", 6);
    /**
     * Covers the variants of bows.
     */
    public static final MaterialSetTag BOWS = new MaterialSetTag(keyFor("bows"))
            .add(Material.BOW)
            .add(Material.CROSSBOW)
            .ensureSize("BOWS", 2);
    /**
     * Covers the variants of player-throwable projectiles (not requiring a bow or any other "assistance").
     */
    public static final MaterialSetTag THROWABLE_PROJECTILES = new MaterialSetTag(keyFor("throwable_projectiles"))
            .add(Material.EGG, Material.SNOWBALL, Material.SPLASH_POTION, Material.TRIDENT, Material.ENDER_PEARL, Material.EXPERIENCE_BOTTLE, Material.FIREWORK_ROCKET);
    /**
     * Covers materials that can be colored, such as wool, shulker boxes, stained glass etc.
     */
    @SuppressWarnings("unchecked")
    public static final MaterialSetTag COLORABLE = new MaterialSetTag(keyFor("colorable"))
            .add(Tag.WOOL, Tag.CARPETS).add(SHULKER_BOXES, STAINED_GLASS, STAINED_GLASS_PANES, CONCRETES, BEDS);
    /**
     * Covers the variants of coral.
     */
    public static final MaterialSetTag CORAL = new MaterialSetTag(keyFor("coral"))
            .endsWith("_CORAL")
            .ensureSize("CORAL", 10);
    //.ensureSize("COLORABLE", 81); unit test don't have the vanilla item tags, so counts don't line up for real
    /**
     * Covers the variants of coral fans.
     */
    public static final MaterialSetTag CORAL_FANS = new MaterialSetTag(keyFor("coral_fans"))
            .endsWith("_CORAL_FAN")
            .endsWith("_CORAL_WALL_FAN")
            .ensureSize("CORAL_FANS", 20);
    /**
     * Covers the variants of coral blocks.
     */
    public static final MaterialSetTag CORAL_BLOCKS = new MaterialSetTag(keyFor("coral_blocks"))
            .endsWith("_CORAL_BLOCK")
            .ensureSize("CORAL_BLOCKS", 10);
    /**
     * Covers all items that can be enchanted from the enchantment table or anvil.
     */
    public static final MaterialSetTag ENCHANTABLE = new MaterialSetTag(keyFor("enchantable"))
            .add(PICKAXES, SWORDS, SHOVELS, AXES, HOES, HELMETS, CHEST_EQUIPPABLE, LEGGINGS, BOOTS, BOWS)
            .add(Material.TRIDENT, Material.SHIELD, Material.FISHING_ROD, Material.SHEARS, Material.FLINT_AND_STEEL, Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK)
            .ensureSize("ENCHANTABLE", 65);
    /**
     * Covers the variants of raw ores.
     */
    public static final MaterialSetTag RAW_ORES = new MaterialSetTag(keyFor("raw_ores"))
            .add(Material.RAW_COPPER, Material.RAW_GOLD, Material.RAW_IRON);
    /**
     * Covers the variants of raw ore blocks.
     */
    public static final MaterialSetTag RAW_ORE_BLOCKS = new MaterialSetTag(keyFor("raw_ore_blocks"))
            .add(Material.RAW_COPPER_BLOCK, Material.RAW_GOLD_BLOCK, Material.RAW_IRON_BLOCK);
    /**
     * Covers all oxidized copper blocks.
     */
    public static final MaterialSetTag OXIDIZED_COPPER_BLOCKS = new MaterialSetTag(keyFor("oxidized_copper_blocks"))
            .startsWith("OXIDIZED_").startsWith("WAXED_OXIDIZED_").ensureSize("OXIDIZED_COPPER_BLOCKS", 8);
    /**
     * Covers all weathered copper blocks.
     */
    public static final MaterialSetTag WEATHERED_COPPER_BLOCKS = new MaterialSetTag(keyFor("weathered_copper_blocks"))
            .startsWith("WEATHERED_").startsWith("WAXED_WEATHERED_").ensureSize("WEATHERED_COPPER_BLOCKS", 8);
    /**
     * Covers all exposed copper blocks.
     */
    public static final MaterialSetTag EXPOSED_COPPER_BLOCKS = new MaterialSetTag(keyFor("exposed_copper_blocks"))
            .startsWith("EXPOSED_").startsWith("WAXED_EXPOSED_").ensureSize("EXPOSED_COPPER_BLOCKS", 8);
    /**
     * Covers all un-weathered copper blocks.
     */
    public static final MaterialSetTag UNAFFECTED_COPPER_BLOCKS = new MaterialSetTag(keyFor("unaffected_copper_blocks"))
            .startsWith("CUT_COPPER").startsWith("WAXED_CUT_COPPER").add(Material.COPPER_BLOCK).add(Material.WAXED_COPPER_BLOCK).ensureSize("UNAFFECTED_COPPER_BLOCKS", 8);
    /**
     * Covers all waxed copper blocks.
     * <p>
     * Combine with other copper-related tags to filter is-waxed or not.
     */
    public static final MaterialSetTag WAXED_COPPER_BLOCKS = new MaterialSetTag(keyFor("waxed_copper_blocks"))
            .add(m -> m.name().startsWith("WAXED_") && m.name().contains("COPPER")).ensureSize("WAXED_COPPER_BLOCKS", 16);
    /**
     * Covers all un-waxed copper blocks.
     * <p>
     * Combine with other copper-related tags to filter is-un-waxed or not.
     */
    public static final MaterialSetTag UNWAXED_COPPER_BLOCKS = new MaterialSetTag(keyFor("unwaxed_copper_blocks"))
            .contains("CUT_COPPER").endsWith("_COPPER").notContains("WAXED").ensureSize("UNWAXED_COPPER_BLOCKS", 16);
    /**
     * Covers all copper block variants.
     */
    public static final MaterialSetTag COPPER_BLOCKS = new MaterialSetTag(keyFor("copper_blocks"))
            .add(WAXED_COPPER_BLOCKS).add(UNWAXED_COPPER_BLOCKS).ensureSize("COPPER_BLOCKS", 32);
    /**
     * Covers all weathering/waxed states of the plain copper block.
     */
    public static final MaterialSetTag FULL_COPPER_BLOCKS = new MaterialSetTag(keyFor("full_copper_blocks"))
            .contains("OXIDIZED_COPPER")
            .contains("WEATHERED_COPPER")
            .contains("EXPOSED_COPPER")
            .contains("COPPER_BLOCK")
            .not(Material.RAW_COPPER_BLOCK)
            .ensureSize("FULL_COPPER_BLOCKS", 8);
    /**
     * Covers all weathering/waxed states of the cut copper block.
     */
    public static final MaterialSetTag CUT_COPPER_BLOCKS = new MaterialSetTag(keyFor("cut_copper_blocks"))
            .endsWith("CUT_COPPER").ensureSize("CUT_COPPER_BLOCKS", 8);
    /**
     * Covers all weathering/waxed states of the cut copper stairs.
     */
    public static final MaterialSetTag CUT_COPPER_STAIRS = new MaterialSetTag(keyFor("cut_copper_stairs"))
            .endsWith("CUT_COPPER_STAIRS").ensureSize("CUT_COPPER_STAIRS", 8);
    /**
     * Covers all weathering/waxed states of the cut copper slab.
     */
    public static final MaterialSetTag CUT_COPPER_SLABS = new MaterialSetTag(keyFor("cut_copper_slabs"))
            .endsWith("CUT_COPPER_SLAB").ensureSize("CUT_COPPER_SLABS", 8);

    private static NamespacedKey keyFor(String key) {
        //noinspection deprecation
        return new NamespacedKey("abilityslots", key + "_settag");
    }
}
