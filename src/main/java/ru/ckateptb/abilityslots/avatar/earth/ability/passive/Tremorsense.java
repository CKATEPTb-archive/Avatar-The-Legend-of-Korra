package ru.ckateptb.abilityslots.avatar.earth.ability.passive;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.earth.EarthElement;
import ru.ckateptb.abilityslots.user.AbilityUser;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Tremorsense",
        displayName = "Tremorsense",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "earth",
        description = "Passive ability that helps EarthBender navigate in the dark.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class Tremorsense extends Ability {
    private Location location;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        Block block = livingEntity.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (livingEntity instanceof Player player) {
            Location location = block.getLocation();
            if (!location.equals(this.location)) {
                if (this.location != null) {
                    player.sendBlockChange(this.location, this.location.getBlock().getBlockData());
                    this.location = null;
                }
                if (EarthElement.isEarthNotLava(user, block)) {
                    player.sendBlockChange(location, Material.GLOWSTONE.createBlockData());
                    this.location = location;
                }
            }
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }
}
