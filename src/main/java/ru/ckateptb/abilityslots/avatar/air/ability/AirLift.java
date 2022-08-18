package ru.ckateptb.abilityslots.avatar.air.ability;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "AirLift",
        displayName = "AirLift",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Маги воздуха могут поднимать своих врагов в воздух",
        instruction = "Задержать шифт",
        cooldown = 7000,
        cost = 5
)
public class AirLift extends Ability {

	
	@ConfigField
    private static long energyCostInterval = 1000;
    
    @ConfigField
    private static int range = 10;
    @ConfigField
    private static int upY = 4;
    @ConfigField
    private static int duration = 10000;
    
    
    
	private Entity target;
	private RemovalConditional removal;
	@Override
	public ActivateResult activate(ActivationMethod paramActivationMethod) {
		
		this.target = user.getTargetEntity(range, false);
		if (target == null || target.getLocation().getBlock().isLiquid()) return ActivateResult.NOT_ACTIVATE;
		
		this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .duration(duration)
                .costInterval(energyCostInterval)
                .custom((user, ability) -> target == null)
                .canUse(() -> target.getLocation())
                .custom((user, ability) -> target.isDead())
                .custom((user, ability) -> target.getLocation().getBlock().isLiquid())
                .sneaking(true)
                .build();
		
		
		
		return ActivateResult.ACTIVATE;
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		
		
		target.setVelocity(target.getVelocity().add(new Vector(0, ( livingEntity.getLocation().getY()+upY-target.getLocation().getY() )/40, 0)));
		
		
		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		user.setCooldown(this);
		user.removeEnergy(this);
	}

}
