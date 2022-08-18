package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "Evade",
        displayName = "Evade",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Вы можете уклоняться от атак",
        instruction = "Нажать ЛКМ",
        cooldown = 1000,
        cost = 5
)
public class Evade extends Ability {


	private RemovalConditional removal;
	private double y=0;
	
	private double upForce;
	private double random;
	
	@ConfigField
    private static long duration = 2000;
	
	@ConfigField
    private static int upY = 4;
	
	@ConfigField
    private static double radius = 1;
	
	@Override
	public ActivateResult activate(ActivationMethod arg0) {
		
		random = Math.toRadians(Math.random()*360);
		
		upForce = ((double) upY / ((double) duration/50)) / 20;
		
		
		this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .duration(duration)
                .build();
		
		return livingEntity.getLocation().getBlock().isLiquid() ? ActivateResult.NOT_ACTIVATE : ActivateResult.ACTIVATE;
	}

	@Override
	public void destroy() {
		
		user.setCooldown(this);
		user.removeEnergy(this);
		
		
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		y += upForce;
		double x = Math.cos((y/upForce)*(Math.PI/8)) * radius;
		double z = Math.sin((y/upForce)*(Math.PI/8)) * radius;
		
		
		
		AirElement.display(livingEntity.getLocation().add(0, -0.2, 0), 3, 0.1f, -0.5f, 0.1f);
		user.setVelocity(new ImmutableVector(x, y, z).rotateAroundY(random), this);
		
		return UpdateResult.CONTINUE;
	}

}
