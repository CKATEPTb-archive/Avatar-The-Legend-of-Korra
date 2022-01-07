package ru.ckateptb.abilityslots.common;

import org.bukkit.Location;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.math.Vector3d;

public interface BurstableAbility extends Ability {
    void initialize(AbilityUser user, Location location, Vector3d direction);
}
