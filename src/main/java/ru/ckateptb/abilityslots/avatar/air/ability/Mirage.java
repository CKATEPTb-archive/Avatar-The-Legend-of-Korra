package ru.ckateptb.abilityslots.avatar.air.ability;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.particle.Particle;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "Mirage",
        displayName = "Mirage",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Маги воздуха могут скрыть своё присутствие потоками воздуха.",
        instruction = "Нажать шифт",
        cooldown = 20000,
        cost = 10
)
public class Mirage extends Ability {

	@ConfigField
    private static long duration = 10000;

    private RemovalConditional removal;
	

    @ConfigField
    private static long energyCostInterval = 1000;
	
	@Override
	public ActivateResult activate(ActivationMethod paramActivationMethod) {
		
		if (user.getEyeLocation().toBlock(world).isLiquid() || !user.getAbilityInstances(Mirage.class).isEmpty()) return ActivateResult.NOT_ACTIVATE;
		
		
		
		removal = new RemovalConditional.Builder()
                .dead()
                .offline()
                .duration(duration/2)
                .costInterval(energyCostInterval)
                .build();
		
		
		
		Particle.CLOUD.display(livingEntity.getLocation(), 20, 2, 2, 2, 0.1);
		AirElement.display(livingEntity.getLocation(), 10, 3, 3, 3, 0.1f);
		
		user.addPotionEffect(this, new PotionEffect(PotionEffectType.INVISIBILITY, (int) (duration/50), 0, false, false, false));
		
		
		return ActivateResult.ACTIVATE;
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		
		AirElement.display(livingEntity.getLocation().add(0, -0.2, 0), 2, 0.3f, 0.4f, 0.3f);
		
		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		
		user.setCooldown(this);
		
	}

}
