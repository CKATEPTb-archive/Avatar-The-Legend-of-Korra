package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsOfflineRemovalPolicy;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.AABB;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.spring.SpringContext;
import ru.ckateptb.tablecloth.temporary.TemporaryService;
import ru.ckateptb.tablecloth.temporary.flight.TemporaryFlight;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSpout",
        displayName = "AirSpout",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Creates a column of air below you that allows you to soar above the ground",
        instruction = "Left Click",
        cooldown = 0
)
@CollisionParticipant
public class AirSpout implements Ability {
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

    private AbilityUser user;
    private LivingEntity livingEntity;

    private AABB collider;
    private TemporaryFlight flight;
    private long nextRenderTime;
    private long startTime;
    private Listener moveHandler;
    private CompositeRemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);

        if(getAbilityInstanceService().destroyInstanceType(user, getClass())) {
            return ActivateResult.NOT_ACTIVATE;
        }

        if (livingEntity.getEyeLocation().getBlock().isLiquid()) {
            return ActivateResult.NOT_ACTIVATE;
        }

        if (WorldUtils.getDistanceAboveGround(livingEntity, false) > height + heightBuffer) {
            return ActivateResult.NOT_ACTIVATE;
        }

        this.nextRenderTime = System.currentTimeMillis();
        this.flight = new TemporaryFlight(livingEntity, duration, true, false, true);
        this.startTime = System.currentTimeMillis();
        this.moveHandler = new MoveHandler(user);
        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new IsOfflineRemovalPolicy(user)
        );
        Bukkit.getPluginManager().registerEvents(moveHandler, AbilitySlots.getInstance());
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if(removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }
        double maxHeight = height + heightBuffer;
        Location location = livingEntity.getLocation();
        if (!user.canUse(location)) {
            return UpdateResult.REMOVE;
        }

        if (livingEntity.getEyeLocation().getBlock().isLiquid()) {
            return UpdateResult.REMOVE;
        }

        if (duration != 0 && System.currentTimeMillis() > this.startTime + duration)
            return UpdateResult.REMOVE;

        Vector3d ground = RayTrace.of(new Vector3d(location), Vector3d.MINUS_J).type(RayTrace.Type.BLOCK).range(maxHeight + 1).ignoreLiquids(false).result(livingEntity.getWorld()).position();

        double distance = WorldUtils.getDistanceAboveGround(livingEntity, false);
        if(distance > maxHeight) {
            return UpdateResult.REMOVE;
        }

        // Remove flight when user goes above the top. This will drop them back down into the acceptable height.
        if(livingEntity instanceof Player player) {
            player.setAllowFlight(!(distance > height));
            player.setFlying(!(distance > height));
        }

        Vector3d mid = ground.add(Vector3d.PLUS_J.multiply(distance / 2.0));

        // Create a bounding box for collision that extends through the spout from the ground to the player.
        collider = new AABB(new Vector3d(-0.5, -distance / 2.0, -0.5), new Vector3d(0.5, distance / 2.0, 0.5)).at(mid);

        render(ground);

        return UpdateResult.CONTINUE;
    }


    private void render(Vector3d ground) {
        long time = System.currentTimeMillis();
        if (time < this.nextRenderTime) return;

        double dy = livingEntity.getLocation().getY() - ground.getY();

        for (int i = 0; i < dy; ++i) {
            Location location = ground.toLocation(livingEntity.getWorld()).add(0, i, 0);
            AirElement.display(location, 3, 0, 0.4f, 0.4f, 0.4f, ThreadLocalRandom.current().nextInt(20) == 0);
        }

        nextRenderTime = time + renderDelay;
    }

    @Override
    public Collection<Collider> getColliders() {
        if (this.collider != null) {
            return Collections.singletonList(this.collider);
        }

        return Collections.emptyList();
    }

    @Override
    public void destroy() {
        SpringContext.getInstance().getBean(TemporaryService.class).revert(flight);
        AbilityInformation information = getInformation();
        this.user.setCooldown(information, information.getCooldown());
        PlayerMoveEvent.getHandlerList().unregister(moveHandler);
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    public static class MoveHandler implements Listener {
        private final AbilityUser user;

        public MoveHandler(AbilityUser user) {
            this.user = user;
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            LivingEntity entity = user.getEntity();
            if(event.getPlayer().equals(entity)) {
                Vector3d velocity = new Vector3d(event.getTo().clone().subtract(event.getFrom())).setY(0);
                if (velocity.length() > maxSpeed) {
                    velocity = velocity.normalize().multiply(maxSpeed);
                    entity.setVelocity(velocity.toBukkitVector());
                }
            }
        }
    }
}
