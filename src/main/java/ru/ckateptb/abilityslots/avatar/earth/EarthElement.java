package ru.ckateptb.abilityslots.avatar.earth;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.avatar.earth.ability.subcategories.LavaBender;
import ru.ckateptb.abilityslots.avatar.earth.ability.subcategories.MetalBender;
import ru.ckateptb.abilityslots.avatar.earth.ability.subcategories.SandBender;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;
import ru.ckateptb.abilityslots.common.paper.MaterialSetTag;
import ru.ckateptb.abilityslots.common.paper.MaterialTags;
import ru.ckateptb.abilityslots.common.paper.PersistentDataLayer;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.ioc.IoC;

@Getter
@Setter
public class EarthElement extends AbstractAbilityCategory {
    public static final MaterialSetTag EARTH_BENDABLE;
    public static final MaterialSetTag SAND_BENDABLE;
    public static final MaterialSetTag METAL_BENDABLE;
    public static final MaterialSetTag LAVA_BENDABLE;
    public static final MaterialSetTag ALL;
    @ConfigField
    @Getter
    private static long revertTime = 60000;

    static {
        NamespacedKey key = PersistentDataLayer.getInstance().NSK_MATERIAL;
        EARTH_BENDABLE = new MaterialSetTag(key)
                .add(Tag.DIRT.getValues())
                .add(Tag.STONE_BRICKS.getValues())
                .add(MaterialTags.TERRACOTTA.getValues())
                .add(MaterialTags.CONCRETES.getValues())
                .add(MaterialTags.CONCRETE_POWDER.getValues())
                .add(Material.DIRT_PATH,
                        Material.GRANITE, Material.POLISHED_GRANITE, Material.DIORITE, Material.POLISHED_DIORITE,
                        Material.ANDESITE, Material.POLISHED_ANDESITE, Material.GRAVEL, Material.CLAY,
                        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
                        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.NETHERRACK, Material.STONE_BRICK_STAIRS,
                        Material.STONE, Material.COBBLESTONE, Material.COBBLESTONE_STAIRS, Material.AMETHYST_BLOCK,
                        Material.DEEPSLATE, Material.CALCITE, Material.TUFF, Material.SMOOTH_BASALT
                ).ensureSize("Earth", 113);

        SAND_BENDABLE = new MaterialSetTag(key)
                .add(Material.SAND, Material.RED_SAND, Material.SOUL_SAND, Material.SOUL_SOIL)
                .add(MaterialTags.SANDSTONES.getValues())
                .add(MaterialTags.RED_SANDSTONES.getValues()).ensureSize("Sand", 12);

        METAL_BENDABLE = new MaterialSetTag(key).add(
                Material.IRON_BLOCK, Material.RAW_IRON_BLOCK,
                Material.GOLD_BLOCK, Material.RAW_GOLD_BLOCK,
                Material.COPPER_BLOCK, Material.RAW_COPPER_BLOCK,
                Material.QUARTZ_BLOCK
        ).ensureSize("Metal", 7);

        LAVA_BENDABLE = new MaterialSetTag(key).add(Material.LAVA, Material.MAGMA_BLOCK).ensureSize("Lava", 2);

        ALL = new MaterialSetTag(key)
                .add(EARTH_BENDABLE.getValues())
                .add(SAND_BENDABLE.getValues())
                .add(METAL_BENDABLE.getValues())
                .add(LAVA_BENDABLE.getValues())
                .ensureSize("All", 134);
    }

    private final String name = "Earth";
    private String displayName = "§2Earth";
    private String prefix = "§2";

    public static void play(Location location) {
        Block block = location.getBlock();
        if (isMetalBendable(block)) {
            play(location, Type.METAL);
        } else if (isLavaBendable(block)) {
            play(location, Type.LAVA);
        } else if (isSandBendable(block)) {
            play(location, Type.SAND);
        } else {
            play(location, Type.EARTH);
        }
    }

    public static void play(Location location, Type type) {
        switch (type) {
            case SAND: {
                playSound(location, Sound.BLOCK_SAND_BREAK, 1, 1);
                return;
            }
            case METAL: {
                playSound(location, Sound.ENTITY_IRON_GOLEM_HURT, 1, 1.25);
                return;
            }
            case LAVA: {
                playSound(location, Sound.BLOCK_LAVA_AMBIENT, 1, 1);
                return;
            }
            default: {
                playSound(location, Sound.ENTITY_GHAST_SHOOT, 1, 1);
            }
        }
    }

    private static void playSound(Location location, Sound sound, double volume, double pitch) {
        location.getWorld().playSound(location, sound, (float) volume, (float) pitch);
    }

    public static boolean isEarthBendable(@NonNull AbilityUser user, @NonNull Block block) {
        if (isMetalBendable(block) && !isMetalBender(user)) {
            return false;
        }
        if (isLavaBendable(block) && !isLavaBender(user)) {
            return false;
        }
        if (isSandBendable(block) && !isSandBender(user)) {
            return false;
        }
        return ALL.isTagged(block);
    }

    public static boolean isEarthNotLava(@NonNull AbilityUser user, @NonNull Block block) {
        if (isLavaBendable(block)) {
            return false;
        }
        if (isMetalBendable(block) && !isMetalBender(user)) {
            return false;
        }
        return ALL.isTagged(block);
    }

    public static boolean isEarthOrSand(@NonNull Block block) {
        return EARTH_BENDABLE.isTagged(block) || SAND_BENDABLE.isTagged(block);
    }

    public static boolean isSandBendable(@NonNull Block block) {
        return SAND_BENDABLE.isTagged(block);
    }

    public static boolean isMetalBendable(@NonNull Block block) {
        return METAL_BENDABLE.isTagged(block);
    }

    public static boolean isLavaBendable(@NonNull Block block) {
        return LAVA_BENDABLE.isTagged(block);
    }

    private static boolean isMetalBender(AbilityUser user) {
        AbilityInstanceService abilityInstanceService = IoC.get(AbilityInstanceService.class);
        return abilityInstanceService.hasAbility(user, MetalBender.class);
    }

    private static boolean isLavaBender(AbilityUser user) {
        AbilityInstanceService abilityInstanceService = IoC.get(AbilityInstanceService.class);
        return abilityInstanceService.hasAbility(user, LavaBender.class);
    }

    private static boolean isSandBender(AbilityUser user) {
        AbilityInstanceService abilityInstanceService = IoC.get(AbilityInstanceService.class);
        return abilityInstanceService.hasAbility(user, SandBender.class);
    }

    public enum Type {
        EARTH,
        SAND,
        METAL,
        LAVA
    }
}
