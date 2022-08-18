package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import org.bukkit.Location;
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
import ru.ckateptb.abilityslots.avatar.air.ability.AirBurst;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.entity.AbilityTargetLiving;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "SummonSelf",
        displayName = "SummonSelf",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Порыв ветра",
        instruction = "AirBlast \\(Tap Shift\\) > AirBlast \\(Tap Shift\\) > AirBurst \\(Left Click\\)",
        cooldown = 7000,
        cost = 30,
        canBindToSlot = false
)

@Sequence({
    @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK),
    @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK_RELEASE),

    @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK),
    @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK_RELEASE),
    
    @AbilityAction(ability = AirBurst.class, action = SequenceAction.LEFT_CLICK)
})
public class SummonSelf extends Ability {

	private RemovalConditional removal;



	private Location location;



	private Location origin;

	
	@ConfigField
    private static int radius = 2;
	
	
    @ConfigField
    private static int speed = 2;
    @ConfigField
    private static int damage = 4;
	
    @ConfigField
    private static int range = 20;
    
	@Override
	public ActivateResult activate(ActivationMethod paramActivationMethod) {
		this.location = livingEntity.getLocation();
		this.origin = this.location.clone();
		
		this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .range(() -> this.origin, () -> this.location, range)
                .build();
		
		user.setVelocity(user.getDirection().multiply(-1), this);
		
		
		user.setCooldown(this);
		user.removeEnergy(this);
		
		return ActivateResult.ACTIVATE;
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		this.location.add(this.origin.getDirection().multiply(speed));
		//if (this.location.distance(this.origin) > range) return UpdateResult.REMOVE;
		
		AirElement.display(location, 20, 2, 1f, 1f, 1f);
		SphereCollider collider = new SphereCollider(world, location.clone().add(0, 1, 0).toVector(), radius);
		
		if (collider.handleEntityCollision(livingEntity, entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                AbilityTargetLiving target = AbilityTarget.of(livingEntity);
                target.damage(damage, this);
            }
            return CollisionCallbackResult.END;
        })) return UpdateResult.REMOVE;
		
		
		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		
		
	}

}
