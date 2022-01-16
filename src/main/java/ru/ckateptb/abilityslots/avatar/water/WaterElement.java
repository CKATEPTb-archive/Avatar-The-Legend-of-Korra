package ru.ckateptb.abilityslots.avatar.water;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;
import ru.ckateptb.abilityslots.common.paper.MaterialSetTag;
import ru.ckateptb.abilityslots.common.paper.MaterialTags;
import ru.ckateptb.abilityslots.common.paper.PersistentDataLayer;
import ru.ckateptb.abilityslots.common.util.MaterialUtils;

@Getter
@Setter
public class WaterElement extends AbstractAbilityCategory {
    public static final MaterialSetTag PLANT_BENDABLE;
    public static final MaterialSetTag ICE_BENDABLE;
    public static final MaterialSetTag SNOW_BENDABLE;
    public static final MaterialSetTag FULL_SOURCES;
    public static final MaterialSetTag ALL;

    static {
        NamespacedKey key = PersistentDataLayer.getInstance().NSK_MATERIAL;
        PLANT_BENDABLE = new MaterialSetTag(key)
                .add(Material.DEAD_BUSH, Material.CACTUS, Material.MELON, Material.VINE)
                .add(Tag.FLOWERS.getValues())
                .add(Tag.SAPLINGS.getValues())
                .add(Tag.CROPS.getValues())
                .add(Tag.LEAVES.getValues())
                .add(MaterialTags.MUSHROOMS.getValues())
                .add(MaterialTags.MUSHROOM_BLOCKS.getValues())
                .add(MaterialTags.PUMPKINS.getValues())
                .ensureSize("Plants", 51);

        ICE_BENDABLE = new MaterialSetTag(key).add(Tag.ICE.getValues()).ensureSize("Ice", 4);

        SNOW_BENDABLE = new MaterialSetTag(key).add(Material.SNOW, Material.SNOW_BLOCK).ensureSize("Snow", 2);

        FULL_SOURCES = new MaterialSetTag(key)
                .add(Material.WATER, Material.CACTUS, Material.MELON, Material.SNOW_BLOCK)
                .add(ICE_BENDABLE.getValues())
                .add(Tag.LEAVES.getValues())
                .add(MaterialTags.MUSHROOM_BLOCKS.getValues())
                .add(MaterialTags.PUMPKINS.getValues())
                .ensureSize("Full Water Sources", 22);

        ALL = new MaterialSetTag(key)
                .add(PLANT_BENDABLE.getValues())
                .add(ICE_BENDABLE.getValues())
                .add(SNOW_BENDABLE.getValues())
                .add(Material.WATER).ensureSize("Waterbendable", 58);
    }

    private final String name = "Water";
    private String displayName = "§1Water";
    private String prefix = "§1";

    public static boolean isWaterBendable(@NonNull Block block) {
        return MaterialUtils.isWater(block) || ALL.isTagged(block);
    }

    public static boolean isWaterNotPlant(@NonNull Block block) {
        return isWaterBendable(block) && !PLANT_BENDABLE.isTagged(block);
    }

    public static boolean isIceBendable(@NonNull Block block) {
        return ICE_BENDABLE.isTagged(block);
    }

    public static boolean isSnowBendable(@NonNull Block block) {
        return SNOW_BENDABLE.isTagged(block);
    }

    public static boolean isWaterOrIceBendable(@NonNull Block block) {
        return MaterialUtils.isWater(block) || ICE_BENDABLE.isTagged(block);
    }

    public static boolean isPlantBendable(@NonNull Block block) {
        return PLANT_BENDABLE.isTagged(block);
    }

    public static boolean isFullWaterSource(@NonNull Block block) {
        return FULL_SOURCES.isTagged(block) || MaterialUtils.isWaterPlant(block);
    }
}
