package ru.ckateptb.abilityslots.avatar.earth.ability.passive;

import com.destroystokyo.paper.MaterialTags;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.earth.EarthElement;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.temporary.block.TemporaryBlock;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.Objects;
import java.util.function.Predicate;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "DensityShift",
        displayName = "DensityShift",
        activationMethods = {ActivationMethod.PASSIVE, ActivationMethod.FALL},
        category = "earth",
        description = "This passive ability prevents EarthBender from taking damage when falling to the earth.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class DensityShift implements Ability {
    @ConfigField
    private static long duration = 6000;
    @ConfigField
    private static double radius = 2.0;

    private AbilityUser user;
    private LivingEntity livingEntity;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        if (method == ActivationMethod.FALL && getAbilityInstanceService().hasAbility(user, getClass())) {
            return shouldPrevent() ? ActivateResult.NOT_ACTIVATE_AND_CANCEL_EVENT : ActivateResult.NOT_ACTIVATE;
        }
        return method == ActivationMethod.PASSIVE ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    private boolean shouldPrevent() {
        Block block = livingEntity.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (!user.canUse(block.getLocation())) return false;
        Location center = block.getLocation().toCenterLocation();
        Predicate<Block> predicate = b -> EarthElement.isEarthBendable(user, b) && b.getRelative(BlockFace.UP).isPassable();
        if (predicate.test(block)) {
            for (Block b : WorldUtils.getNearbyBlocks(center, radius, predicate)) {
                new TemporaryBlock(b.getLocation(), getSoftType(b.getBlockData()), duration);
            }
            return true;
        }
        return false;
    }

    // Finds a suitable soft block type to replace a solid block
    public BlockData getSoftType(BlockData data) {
        Material blockMaterial = data.getMaterial();
        if (blockMaterial == Material.SAND || MaterialTags.SANDSTONES.isTagged(data)) {
            return Material.SAND.createBlockData();
        } else if (blockMaterial == Material.RED_SAND || MaterialTags.RED_SANDSTONES.isTagged(data)) {
            return Material.RED_SAND.createBlockData();
        } else if (MaterialTags.STAINED_TERRACOTTA.isTagged(data)) {
            return Material.CLAY.createBlockData();
        } else if (MaterialTags.CONCRETES.isTagged(data)) {
            Material material = Material.getMaterial(blockMaterial.name() + "_POWDER");
            return Objects.requireNonNullElse(material, Material.GRAVEL).createBlockData();
        }
        return switch (blockMaterial) {
            case STONE, GRANITE, POLISHED_GRANITE, DIORITE, POLISHED_DIORITE, ANDESITE, POLISHED_ANDESITE,
                    GRAVEL, DEEPSLATE, CALCITE, TUFF, SMOOTH_BASALT -> Material.GRAVEL.createBlockData();
            case DIRT, MYCELIUM, GRASS_BLOCK, DIRT_PATH, PODZOL, COARSE_DIRT, ROOTED_DIRT -> Material.COARSE_DIRT.createBlockData();
            default -> Material.SAND.createBlockData();
        };
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
