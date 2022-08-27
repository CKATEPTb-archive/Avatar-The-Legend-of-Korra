package ru.ckateptb.abilityslots.avatar.air.ability;

import org.bukkit.util.Vector;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "Flatuate",
        displayName = "Flatuate",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Ээээ не придумал.",
        instruction = "Нажать шифт",
        cooldown = 5000,
        cost = 10
)
public class Flatuate extends Ability {

	@ConfigField
	private static double force = 1;
	
	@ConfigField
	private static double height = 2;

	private RemovalConditional removal;
	
	@Override
	public ActivateResult activate(ActivationMethod paramActivationMethod) {
		
		
		this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .duration((long) ((1000*force)/2))
                .build();
		
		
		
		user.setVelocity(user.getDirection().setY(height).normalize().multiply(force), this);
		user.setCooldown(this);
		user.removeEnergy(this);
		return ActivateResult.ACTIVATE;
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		AirElement.display(livingEntity.getLocation().add(0, -0.2, 0), 3, 0.1f, -0.5f, 0.1f);
		
		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		
		if (user.isSneaking())
		{
			user.setVelocity(new Vector(0, force, 0), this);
			AirElement.display(livingEntity.getLocation().add(0, -0.2, 0), 9, 2, 0.5f, -0.9f, 0.5f);
		}
		
	}

}
