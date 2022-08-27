package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import lombok.Getter;
import ru.ckateptb.abilityslots.AbilitySlots;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.AirScooter;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.entity.AbilityTargetLiving;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "AirWheel",
        displayName = "AirWheel",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Создайте попутный ветер позади себя чтобы ускориться",
        instruction = "AirBlast \\(Hold Sneak\\) > AirBurst \\(Release Sneak\\)",
        cooldown = 7000,
        cost = 20,
        canBindToSlot = false
)
@Sequence({
	
    @AbilityAction(ability = AirScooter.class, action = SequenceAction.SNEAK),
    @AbilityAction(ability = AirScooter.class, action = SequenceAction.SNEAK_RELEASE),
    @AbilityAction(ability = AirScooter.class, action = SequenceAction.SNEAK),
    @AbilityAction(ability = AirScooter.class, action = SequenceAction.SNEAK_RELEASE),
    @AbilityAction(ability = AirScooter.class, action = SequenceAction.LEFT_CLICK)
    
    
})
public class AirWheel extends Ability {

	@ConfigField
    private static double speed = 1;
	
	@ConfigField
    private static double damage = 2;
	
	
	@ConfigField
    private static long duration = 7000;
	
	@ConfigField
    private static boolean withoutSprinting = false;
	@ConfigField
    private static boolean onDamageDeactivate = false;
	@ConfigField
	private static double forceKnockback;
	
	@ConfigField
    private static long energyCostInterval = 1000;

	private RemovalConditional removal;

	private Listener damageHandler;

	private HeightSmoother heightSmoother;

	private int stuckCount;
	private double verticalPosition = 0;
	private boolean canRender = true;

	
	
	@Override
	public ActivateResult activate(ActivationMethod arg0) {
		double dist = user.getDistanceAboveGround();
		if (dist < 0.5 || dist > 5) return ActivateResult.NOT_ACTIVATE;
		this.heightSmoother = new HeightSmoother();
		user.destroyInstances(AirScooter.class);
		removal = new RemovalConditional.Builder()
                .dead()
                .offline()
                .duration(duration)
                .world()
                .sneaking(false)
                .canUse(() -> livingEntity.getLocation())
                .costInterval(energyCostInterval)
                .build();
		
		
		if (!livingEntity.getLocation().getBlock().isLiquid() && (withoutSprinting || user.isSprinting())) {
			if (onDamageDeactivate)
			{
				this.damageHandler = new DamageHandler();
	            Bukkit.getPluginManager().registerEvents(damageHandler, AbilitySlots.getInstance());
			}
	        return ActivateResult.ACTIVATE;
        }
		
		return ActivateResult.NOT_ACTIVATE;
		}

	@Override
	public void destroy() {
		if (onDamageDeactivate) EntityDamageEvent.getHandlerList().unregister(damageHandler);
		user.setCooldown(this);
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		this.stuckCount = new ImmutableVector(livingEntity.getVelocity()).lengthSquared() < 0.1 ? stuckCount + 1 : 0;
        if (stuckCount > 10 || !move()) {
            return UpdateResult.REMOVE;
        }
        if (canRender ) {
            render();
        }
		
        SphereCollider collider = new SphereCollider(world, user.getLocation(), 1);
        
        collider.handleEntityCollision(livingEntity, false, (entity) -> {
        	 if (entity instanceof LivingEntity livingEntity) {
                 AbilityTargetLiving target = AbilityTarget.of(livingEntity);
                 target.damage(damage, this);
                 target.setVelocity(target.getEyeLocation().subtract(user.getLocation()).add(0, 0.4, 0).normalize().multiply(forceKnockback), this);
             }
        	
            return CollisionCallbackResult.CONTINUE;
        });
        
        
		return UpdateResult.CONTINUE;
	}
	
	
	
	
	public void render() {
        
		
		CompletableFuture.runAsync(() -> {
			for (int i = 0; i <= 10; i++)
			{
				ImmutableVector location = user.getDirection().normalize();
				location = location.rotateAroundZ(Math.toRadians(i*36) * location.getX() ).rotateAroundX(Math.toRadians(i*36) * location.getZ()).multiply(2);
				AirElement.display(livingEntity.getEyeLocation().add(location), 7, 0.2f, 0.2f, 0.2f);
			}
        });
		
        
        AirElement.sound(livingEntity.getLocation());
    }
	
	

    private boolean move() {
        if (isColliding()) {
            return false;
        }
        double height = user.getDistanceAboveGround();
        double smoothedHeight = heightSmoother.add(height);
        if (livingEntity.getLocation().getBlock().isLiquid()) {
            height = 0.5;
        } else if (smoothedHeight > 3.25) {
            return false;
        }
        double delta = getPrediction() - height;
        double force = Math.max(-0.5, Math.min(0.5, 0.3 * delta));
        ImmutableVector velocity = user.getDirection().setY(0).normalize().multiply(speed).setY(force);
        AbilityTarget.of(livingEntity).setVelocity(velocity, this);
        livingEntity.setFallDistance(0);
        return true;
    }

    private boolean isColliding() {
        double speed = livingEntity.getVelocity().setY(0).length();
        ImmutableVector direction = user.getDirection().setY(0).normalize(ImmutableVector.ZERO);
        ImmutableVector front = user.getEyeLocation().subtract(0, 0.5, 0)
                .add(direction.multiply(Math.max(speed, AirWheel.speed)));
        Block block = front.toBlock(world);
        return !block.isLiquid() && !block.isPassable();
    }

    private double getPrediction() {
        double playerSpeed = livingEntity.getVelocity().setY(0).length();
        double speed = Math.max(AirWheel.speed, playerSpeed) * 3;
        ImmutableVector offset = user.getDirection().setY(0).normalize().multiply(speed);
        ImmutableVector location = user.getLocation().add(offset);
        AxisAlignedBoundingBoxCollider alignedBoundingBoxCollider = new AxisAlignedBoundingBoxCollider(livingEntity).at(location);
        if (alignedBoundingBoxCollider.handleBlockCollisions(false)) {
            return 2.25;
        }
        return 1.25;
    }
	
	
	
	
	
	
	public class DamageHandler implements Listener {
        @EventHandler
        public void on(EntityDamageEvent event) {
            if (event.getEntity() instanceof LivingEntity entity && entity == livingEntity) {
                user.destroyInstances(AirScooter.class);
            }
        }
    }
	private static class HeightSmoother {
        private final double[] values;
        private int index;

        private HeightSmoother() {
            index = 0;
            values = new double[10];
        }

        private double add(double value) {
            values[index] = value;
            index = (index + 1) % values.length;
            return get();
        }

        private double get() {
            return Arrays.stream(values).sum() / values.length;
        }
    }

}
