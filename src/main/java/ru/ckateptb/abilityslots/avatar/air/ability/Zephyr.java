package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.LivingEntity;
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
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.particle.Particle;
import ru.ckateptb.tablecloth.util.WorldUtils;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "Zephyr",
        displayName = "Zephyr",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "С помощью этой способность все, кто находятся в определённом радиусе вокруг вас, могут левитировать",
        instruction = "Зажать шифт",
        cooldown = 0,
        cost = 40
)
public class Zephyr extends Ability {
	

	@ConfigField
    private static long energyCostInterval = 1000;
	private RemovalConditional removal;
	
	@ConfigField
    private static int radius = 4;
	
	@ConfigField
    private static int amplifier = 1;
	
	
	@Override
	public ActivateResult activate(ActivationMethod arg0) {
		
		this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .costInterval(energyCostInterval)
                .canUse(() -> livingEntity.getLocation())
                .sneaking(true)
                .build();
		if (user.getLocation().toBlock(world).isLiquid()) return ActivateResult.NOT_ACTIVATE;
		user.removeEnergy(this);
		return ActivateResult.ACTIVATE;
	}

	@Override
	public void destroy() {
		
		
		
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		SphereCollider collider = new SphereCollider(world, user.getLocation(), radius);
		
		collider.handleEntityCollision(null, false, (entity) -> {
			if (entity instanceof LivingEntity e)
			{
				((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 10, amplifier));
				if (!WorldUtils.isOnGround(entity)) Particle.CLOUD.display(entity.getLocation().add(0, -0.2, 0));
			}
			
            return CollisionCallbackResult.CONTINUE;
        });
		
		
		CompletableFuture.runAsync(() -> {
			
			for (int i = 0; i < 10; i++)
			{
				double x = Math.cos(Math.toRadians(i*36))*radius;
				double z = Math.sin(Math.toRadians(i*36))*radius;
				
				AirElement.display(livingEntity.getLocation().add(x, 0, z), 3, 0.6, 0.1f, 0f, 0.1f);
			}
			
        });
		
		
		
		return UpdateResult.CONTINUE;
	}

}
