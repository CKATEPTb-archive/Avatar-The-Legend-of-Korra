package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Entity;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "GaleGust",
        displayName = "GaleGust",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Нажмите ЛКМ чтобы создать поток воздуха, который захватывает ваших врагов",
        instruction = "Зажмите шифт чтобы управлять потоком",
        cooldown = 7000,
        cost = 20
)
@CollisionParticipant
public class GaleGust extends Ability {
	@ConfigField
    private static double speed = 0.5;
    @ConfigField
    private static double range = 40;
    @ConfigField
    private static double radius = 2;
    @ConfigField
    private static long entityDuration = 4000;
    @ConfigField
    private static long duration = 7000;
    
    private ImmutableVector location;
    private ImmutableVector origin;
    private RemovalConditional removal;
    
    @ConfigField
    private static long energyCostInterval = 1000;
    
    private final Map<Entity, Long> affected = new HashMap<>();
    private final List<Entity> immune = new ArrayList<>();

    private SphereCollider collider;
    private ImmutableVector target;
    
	@Override
	public ActivateResult activate(ActivationMethod arg0) {
		
		this.location = user.getEyeLocation();
        this.origin = this.location;
		this.target = user.findPosition(range, false, entity -> false, blocks -> true);
        removal = new RemovalConditional.Builder()
                .dead()
                .offline()
                .duration(duration)
                .world()
                .costInterval(energyCostInterval)
                .build();
        
		
        return origin.toBlock(world).isLiquid() || !user.getAbilityInstances(GaleGust.class).isEmpty() ? ActivateResult.NOT_ACTIVATE : ActivateResult.ACTIVATE;
	}

	@Override
	public void destroy() {
		
		user.setCooldown(this);
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		long time = System.currentTimeMillis();
		
		
        ImmutableVector direction = getDirection();
		
        location = location.add(direction.multiply(speed));
        
        
        collider = new SphereCollider(world, location, radius);
        if (location.distance(origin) > range || new SphereCollider(world, location, 0.1).handleBlockCollisions(false)) {
            return UpdateResult.REMOVE;
        }
        
        AirElement.display(location.toLocation(world), 10, 0.4f, 0.4f, 0.4f);
        
        
        collider.handleBlockCollisions(false, false, block -> {
            AirElement.handleBlockInteractions(user, block);
            return CollisionCallbackResult.CONTINUE;
        }, block -> user.canUse(block.getLocation()));

        
        
        collider.handleEntityCollision(livingEntity, false, (entity) -> {
            if (!affected.containsKey(entity) && !immune.contains(entity)) {
                affected.put(entity, System.currentTimeMillis() + entityDuration);
            }
            return CollisionCallbackResult.CONTINUE;
        });
        
        
        for (Iterator<Map.Entry<Entity, Long>> iterator = affected.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Entity, Long> entry = iterator.next();
            Entity entity = entry.getKey();
            AbilityTarget target = AbilityTarget.of(entity);
            ImmutableVector targetLocation = target.getLocation();
            long end = entry.getValue();
            if (targetLocation.distance(this.location) > 5 || time > end) {
                if (time > end) {
                    immune.add(entity);
                }
                iterator.remove();
                continue;
            }
            entity.setFallDistance(0);
            ImmutableVector force = this.location.subtract(targetLocation);
            if (force.lengthSquared() == 0) {
                continue;
            }

            force = force.normalize().multiply(speed);
            target.setVelocity(force, this);
        }
        
        
        
        return UpdateResult.CONTINUE;
	}
	
	
	private ImmutableVector getDirection() {
		target = user.isSneaking() ?  user.findPosition(range, false, entity -> false, blocks -> true) : target;
        ImmutableVector direction = target.subtract(location);

        if (direction.lengthSquared() > 0) {
            direction = direction.normalize();
        }

        return direction;
    }
	
	@Override
    public Collection<Collider> getColliders() {
        return collider == null ? Collections.emptyList() : Collections.singleton(collider);
    }
}
