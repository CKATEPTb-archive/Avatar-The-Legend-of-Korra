package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.air.ability.AirShield;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "SwiftStream",
        displayName = "SwiftStream",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Порыв ветра",
        instruction = "AirShield \\(Tap Sneak\\) > AirBlast \\(Left Click\\) > AirBlast \\(Left Click\\)",
        cooldown = 7000,
        cost = 10,
        canBindToSlot = false
)
@Sequence({
    @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK),
    @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK_RELEASE),
    @AbilityAction(ability = AirBlast.class, action = SequenceAction.LEFT_CLICK),
    @AbilityAction(ability = AirBlast.class, action = SequenceAction.LEFT_CLICK)
})
public class SwiftStream extends Ability {

	
	private RemovalConditional removal;
	private final Map<Entity, Double> affected = new HashMap<>();

	@ConfigField
	private static double force = 7;
	
	@ConfigField
	private static double radius = 5;
	
	@Override
	public ActivateResult activate(ActivationMethod paramActivationMethod) {
		user.destroyInstances(AirBlast.class);
		this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .duration((long) ((1000*force)/2))
                .build();
		
		SphereCollider collider = new SphereCollider(world, user.getLocation(), radius);
		
		collider.handleEntityCollision(null, false, (entity) -> {
			if (entity instanceof LivingEntity e)
			{
				if (!affected.containsKey(entity)) {
	                affected.put(entity, force);
	            }
				entity.setVelocity(user.getDirection().multiply(force));
			}
			
            return CollisionCallbackResult.CONTINUE;
        });

		user.setCooldown(this);
		user.removeEnergy(this);
		
		
		
		
		
		
		
		return ActivateResult.ACTIVATE;
	}

	@Override
	public void destroy() {
		
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		if (livingEntity.getVelocity().getY() < 0) return UpdateResult.REMOVE;
		
		
		
		affected.forEach((entity, force) -> AirElement.display(entity.getLocation().add(0, -0.2, 0), 9, 2, 0.5f, -0.9f, 0.5f));
		
		
		return UpdateResult.CONTINUE;
	}

}
