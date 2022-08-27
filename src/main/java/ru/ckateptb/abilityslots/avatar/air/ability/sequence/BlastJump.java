package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.air.ability.AirSuction;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.particle.Particle;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "BlastJump",
        displayName = "BlastJump",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Маги воздуха могут совершать прыжки с места или с воздуха",
        instruction = "AirSuction \\(Hold Sneak\\) > AirBlast \\(Release Sneak\\)",
        cooldown = 7000,
        cost = 20,
        canBindToSlot = false
)
@Sequence({
    @AbilityAction(ability = AirSuction.class, action = SequenceAction.SNEAK),
    @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK_RELEASE)
})
public class BlastJump extends Ability {

	 @ConfigField
	 private static double force = 2;
	
	
	@Override
	public ActivateResult activate(ActivationMethod paramActivationMethod) {
		
		
		
		Particle.CLOUD.display(livingEntity.getLocation(), 20, 0.2, 0.2, 0.2, 0.1);
		livingEntity.setVelocity(livingEntity.getLocation().getDirection().normalize().multiply(force));
		user.setCooldown(this);
		user.removeEnergy(this);
		return ActivateResult.ACTIVATE;
	}

	@Override
	public void destroy() {
		
		
	}

	@Override
	public UpdateResult update() {
		return UpdateResult.REMOVE;
	}
	

}
