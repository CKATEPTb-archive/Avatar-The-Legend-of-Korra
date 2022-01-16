package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.ckateptb.abilityslots.AbilitySlots;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.spring.SpringContext;
import ru.ckateptb.tablecloth.temporary.TemporaryService;
import ru.ckateptb.tablecloth.temporary.flight.TemporaryFlight;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSpout",
        displayName = "AirSpout",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Creates a column of air below you that allows you to soar above the ground",
        instruction = "Left Click",
        cooldown = 0,
        cost = 10
)
@CollisionParticipant
public class AirSpout extends Ability {
    @ConfigField
    private static long duration = 0;
    @ConfigField
    private static double height = 12;
    @ConfigField
    private static double heightBuffer = 2;
    @ConfigField
    private static double maxSpeed = 0.2;
    @ConfigField
    private static int renderDelay = 100;
    @ConfigField
    private static long energyCostInterval = 1000;

    private AxisAlignedBoundingBoxCollider collider;
    private TemporaryFlight flight;
    private long nextRenderTime;
    private long startTime;
    private Listener moveHandler;
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        if (getAbilityInstanceService().destroyInstanceType(user, getClass())
                || livingEntity.getEyeLocation().getBlock().isLiquid()
                || user.getDistanceAboveGround() > heightBuffer + heightBuffer) {
            return ActivateResult.NOT_ACTIVATE;
        }
        this.nextRenderTime = System.currentTimeMillis();
        this.flight = new TemporaryFlight(livingEntity, duration, true, false, true);
        this.startTime = System.currentTimeMillis();
        this.moveHandler = new MoveHandler(user, this);
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .canUse(() -> livingEntity.getLocation())
                .costInterval(energyCostInterval)
                .custom((user, ability) -> livingEntity.getEyeLocation().getBlock().isLiquid())
                .duration(duration)
                .build();
        Bukkit.getPluginManager().registerEvents(moveHandler, AbilitySlots.getInstance());
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) {
            return UpdateResult.REMOVE;
        }
        double maxHeight = height + heightBuffer;
        ImmutableVector location = user.getLocation();
        Block block = location.getFirstRelativeBlock(world, BlockFace.DOWN, maxHeight + 1);
        if (!block.isLiquid() && block.isPassable()) return UpdateResult.REMOVE;
        ImmutableVector ground = location.setY(block.getY());

        double distance = user.getDistanceAboveGround();
        if (distance > maxHeight) {
            return UpdateResult.REMOVE;
        }

        // Remove flight when user goes above the top. This will drop them back down into the acceptable height.
        if (livingEntity instanceof Player player) {
            player.setAllowFlight(!(distance > height));
            player.setFlying(!(distance > height));
        }

        ImmutableVector mid = ground.add(ImmutableVector.PLUS_J.multiply(distance / 2.0));

        // Create a bounding box for collision that extends through the spout from the ground to the player.
        collider = new AxisAlignedBoundingBoxCollider(world, new ImmutableVector(-0.5, -distance / 2.0, -0.5), new ImmutableVector(0.5, distance / 2.0, 0.5)).at(mid);

        render(ground);

        return UpdateResult.CONTINUE;
    }

    @Override
    public Collection<Collider> getColliders() {
        return this.collider == null ? Collections.emptyList() : Collections.singletonList(this.collider);
    }

    private void render(ImmutableVector ground) {
        long time = System.currentTimeMillis();
        if (time < this.nextRenderTime) return;

        double dy = livingEntity.getLocation().getY() - ground.getY();
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < dy; ++i) {
                Location location = ground.toLocation(livingEntity.getWorld()).add(0, i, 0);
                AirElement.display(location, 3, 0.4f, 0.4f, 0.4f, false);
            }
            AirElement.sound(livingEntity.getLocation());
        });

        nextRenderTime = time + renderDelay;
    }

    @Override
    public void destroy() {
        SpringContext.getInstance().getBean(TemporaryService.class).revert(flight);
        this.user.setCooldown(this);
        PlayerMoveEvent.getHandlerList().unregister(moveHandler);
    }

    public static class MoveHandler implements Listener {
        private final AbilityUser user;
        private final AirSpout spout;

        public MoveHandler(AbilityUser user, AirSpout spout) {
            this.user = user;
            this.spout = spout;
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            LivingEntity entity = user.getEntity();
            if (event.getPlayer().equals(entity)) {
                ImmutableVector velocity = new ImmutableVector(event.getTo().clone().subtract(event.getFrom())).setY(0);
                if (velocity.length() > maxSpeed) {
                    velocity = velocity.normalize().multiply(maxSpeed);
                    AbilityTarget.of(entity).setVelocity(velocity, spout);
                }
            }
        }
    }
}
