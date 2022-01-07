package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.air.ability.AirShield;
import ru.ckateptb.abilityslots.avatar.air.ability.Tornado;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.AABB;
import ru.ckateptb.tablecloth.collision.collider.Disc;
import ru.ckateptb.tablecloth.collision.collider.OBB;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Twister",
        displayName = "Twister",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 4000
)
@Sequence({
        @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK_RELEASE),
        @AbilityAction(ability = Tornado.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.LEFT_CLICK)
})
public class Twister implements Ability {
    @ConfigField
    private static long duration = 8000;
    @ConfigField
    private static double radius = 3.5;
    @ConfigField
    private static double height = 8;
    @ConfigField
    private static double range = 25;
    @ConfigField
    private static double speed = 0.35;
    @ConfigField
    private static double proximity = 2.0;
    @ConfigField
    private static double renderSpeed = 2.5;
    @ConfigField
    private static int streams = 6;
    @ConfigField
    private static int particlesPerStream = 7;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private World world;
    private long startTime;
    private Vector3d base;
    private Vector3d direction;
    private Vector3d origin;
    private double currentHeight;
    private final List<Collider> colliders = new ArrayList<>();
    private final Set<Entity> affected = new HashSet<>();

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);

        AbilityInstanceService instanceService = getAbilityInstanceService();
        for (AirBlast blast : instanceService.getAbilityUserInstances(user, AirBlast.class)) {
            instanceService.destroyInstance(user, blast);
        }

        this.world = livingEntity.getWorld();
        this.startTime = System.currentTimeMillis();
        this.direction = new Vector3d(livingEntity.getEyeLocation().getDirection()).setY(0).normalize(Vector3d.PLUS_I);

        this.base = new Vector3d(livingEntity.getLocation()).add(direction.multiply(2));
        this.base = RayTrace.of(base.add(new Vector3d(0, 3.5, 0)), Vector3d.MINUS_J).range(7).ignoreLiquids(false).result(world).position();

        if (!isAcceptableBase()) {
            return ActivateResult.NOT_ACTIVATE;
        }

        this.origin = base;
        this.currentHeight = height;

        AbilityInformation information = getInformation();
        user.setCooldown(information, information.getCooldown());
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (base.distance(origin) > range) {
            if (System.currentTimeMillis() > startTime + duration)
                return UpdateResult.REMOVE;
        } else {
            base = base.add(direction.multiply(speed));
            base = RayTrace.of(base.add(new Vector3d(0, 3.5, 0)), Vector3d.MINUS_J).range(currentHeight).ignoreLiquids(false).result(world).position();
        }
        if (!isAcceptableBase()) {
            return UpdateResult.REMOVE;
        }


        if (!user.canUse(base.toLocation(world))) {
            return UpdateResult.REMOVE;
        }

        Vector3d top = RayTrace.of(base, Vector3d.PLUS_J).range(currentHeight).ignoreLiquids(false).result(world).position();

        currentHeight = top.getY() - base.getY();
        if (currentHeight <= 0) {
            return UpdateResult.REMOVE;
        }

        render();

        colliders.clear();
        for (int i = 0; i < currentHeight - 1; ++i) {
            Vector3d location = base.add(new Vector3d(0, i, 0));
            double r = proximity + radius * (i / currentHeight);
            AABB aabb = new AABB(new Vector3d(-r, 0, -r), new Vector3d(r, 1, r)).at(location);

            colliders.add(new Disc(new OBB(aabb), new Sphere(location, r)));
        }

        for (Collider collider : colliders) {
            CollisionUtils.handleEntityCollisions(livingEntity, collider, (entity) -> {
                if (user.canUse(entity.getLocation())) {
                    affected.add(entity);
                }
                return false;
            }, false);
        }

        for (Entity entity : affected) {
            Vector3d forceDirection = new Vector3d(base.toLocation(world).add(0, currentHeight, 0).subtract(entity.getLocation())).normalize();
            Vector3d force = forceDirection.multiply(speed);
            entity.setVelocity(force.toBukkitVector());
        }

        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public List<Collider> getColliders() {
        return colliders;
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    private void render() {
        long time = System.currentTimeMillis();
        double cycleMS = (1000.0 * currentHeight / renderSpeed);

        for (int j = 0; j < streams; ++j) {
            double thetaOffset = j * ((Math.PI * 2) / streams);

            for (int i = 0; i < particlesPerStream; ++i) {
                double thisTime = time + (i / (double) particlesPerStream) * cycleMS;
                double f = (thisTime - startTime) / cycleMS % 1.0;

                double y = f * currentHeight;
                double theta = y + thetaOffset;

                double x = radius * f * Math.cos(theta);
                double z = radius * f * Math.sin(theta);

                if (y > currentHeight) {
                    continue;
                }

                Location current = base.toLocation(world).add(x, y, z);
                AirElement.display(current, 1, 0.0f, 0.0f, 0.0f, 0.0f, ThreadLocalRandom.current().nextInt(20) == 0);
            }
        }
    }

    private boolean isAcceptableBase() {
        Block block = base.toBlock(world);
        // Remove if base couldn't be resolved to a non-solid block.
        if (AABB.from(block).contains(base)) {
            return false;
        }

        Block below = block.getRelative(BlockFace.DOWN);
        // Remove if base was resolved to being above a non-solid block.
        return AABB.from(below).max != null || below.isLiquid();
    }
}
