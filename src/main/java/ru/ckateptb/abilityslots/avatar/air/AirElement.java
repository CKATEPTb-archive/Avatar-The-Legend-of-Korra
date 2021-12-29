package ru.ckateptb.abilityslots.avatar.air;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;
import ru.ckateptb.abilityslots.particle.ParticleEffect;
import ru.ckateptb.abilityslots.user.AbilityUser;

@Getter
@Setter
public class AirElement extends AbstractAbilityCategory {
    private final String name = "Air";
    private String displayName = "§7Air";
    private String prefix = "§7";

    public static void display(Location location, int amount, float offsetX, float offsetY, float offsetZ) {
        ParticleEffect.SPELL.display(location, amount, offsetX, offsetY, offsetZ);
        World world = location.getWorld();
        if(world != null) world.playSound(location, Sound.ENTITY_CREEPER_HURT, 1, 2);
    }
}
