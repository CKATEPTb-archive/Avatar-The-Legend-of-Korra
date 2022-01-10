package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.conditional.AbilityConditional;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.service.AbilityUserService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.AABB;
import ru.ckateptb.tablecloth.collision.collider.Ray;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.spring.SpringContext;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Suffocate",
        displayName = "Suffocate",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "The ability requires high concentration, but its power is overwhelming! Targets affected by this ability begin to choke, which is why they lose concentration and the ability to use abilities. All they have to do is run!",
        instruction = "Hold Sneak",
        cooldown = 5000
)
public class Suffocate implements Ability {
    @ConfigField
    private static long chargeTime = 500;
    @ConfigField
    private static double range = 20;
    @ConfigField
    private static double selectRange = 6;
    @ConfigField
    private static double selectScale = 2;
    @ConfigField
    private static boolean requireConstantAim = true;
    @ConfigField
    private static double constantAimRadius = 5;
    @ConfigField
    private static double damageAmount = 2;
    @ConfigField
    private static int damageDelay = 2000;
    @ConfigField
    private static int damageInterval = 1000;
    @ConfigField
    private static int slowAmplifier = 1;
    @ConfigField
    private static int slowDelay = 500;
    @ConfigField
    private static int slowInterval = 1250;
    @ConfigField
    private static int blindAmplifier = 1;
    @ConfigField
    private static int blindDelay = 2000;
    @ConfigField
    private static int blindInterval = 1500;
    @ConfigField
    private static int renderRadiusScaleTime = 5000;
    @ConfigField
    private static int renderLayerScaleTime = 7500;
    @ConfigField
    private static double renderMaxRadius = 3;
    @ConfigField
    private static double renderMinRadius = 0.5;
    @ConfigField
    private static int renderLayers = 5;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private LivingEntity target;
    private long startTime;
    private long nextDamageTime;
    private long nextSlowTime;
    private long nextBlindTime;
    private boolean started;
    private CompositeRemovalPolicy removalPolicy;
    private SuffocatingConditional conditional;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        this.startTime = System.currentTimeMillis();
        this.started = false;

        this.target = (LivingEntity) RayTrace.of(livingEntity)
                .range(selectRange)
                .type(RayTrace.Type.ENTITY)
                .raySize(selectScale)
                .filter(e -> e instanceof LivingEntity && e != livingEntity)
                .result(livingEntity.getWorld()).entity();

        if (target == null) {
            return ActivateResult.NOT_ACTIVATE;
        }

        if (!user.canUse(target.getLocation())) {
            return ActivateResult.NOT_ACTIVATE;
        }

        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new OutOfRangeRemovalPolicy(() -> livingEntity.getLocation(), () -> target.getLocation(), range),
                new SwappedSlotsRemovalPolicy<>(user, Suffocate.class),
                new OutOfWorldRemovalPolicy(user),
                new SneakingRemovalPolicy(user, true)
        );

        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        long time = System.currentTimeMillis();

        if (this.removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }

        if (time < this.startTime + chargeTime) {
            return UpdateResult.CONTINUE;
        }

        if (!user.canUse(target.getLocation())) {
            return UpdateResult.REMOVE;
        }

        if (!this.started) {
            this.started = true;
            this.nextDamageTime = startTime + damageDelay;
            this.nextSlowTime = startTime + slowDelay;
            this.nextBlindTime = startTime + blindDelay;

            AbilityUser target = SpringContext.getInstance().getBean(AbilityUserService.class).getAbilityUser(this.target);
            if (target != null) {
                // Prevent the user from bending.
                conditional = new SuffocatingConditional();
                target.getAbilityActivateConditional().add(conditional);
            }
        }

        if (requireConstantAim) {
            AABB bounds = new AABB(new Vector3d(-0.5, -0.5, -0.5), new Vector3d(0.5, 0.5, 0.5))
                    .scale(constantAimRadius)
                    .at(new Vector3d(target.getLocation()));
            Location eyeLocation = livingEntity.getEyeLocation();
            Ray ray = new Ray(new Vector3d(eyeLocation), new Vector3d(eyeLocation.getDirection()));
            if (!bounds.intersects(ray)) {
                return UpdateResult.REMOVE;
            }
        }

        handleEffects();
        render();

        return UpdateResult.CONTINUE;
    }

    private void handleEffects() {
        long time = System.currentTimeMillis();

        if (damageAmount > 0 && time > this.nextDamageTime) {
            target.damage(damageAmount, livingEntity);
            this.nextDamageTime = time + damageInterval;
        }

        if (slowAmplifier > 0 && time >= this.nextSlowTime) {
            target.removePotionEffect(PotionEffectType.SLOW);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowInterval / 50, slowAmplifier - 1, true, false));
            this.nextSlowTime = time + slowInterval;
        }

        if (blindAmplifier > 0 && time >= this.nextBlindTime) {
            target.removePotionEffect(PotionEffectType.BLINDNESS);
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, slowInterval / 50, slowAmplifier - 1, true, false));
            this.nextBlindTime = time + blindInterval;
        }
    }

    private void render() {
        long time = System.currentTimeMillis();
        double rt = (time - startTime) / (double) renderRadiusScaleTime;
        double lt = (time - startTime) / (double) renderLayerScaleTime;
        rt = Math.min(1.0, rt);
        lt = Math.min(1.0, lt);

        double r = Math.max(renderMinRadius, renderMaxRadius * (1.0 - rt));
        double height = r * 2;
        double maxLayers = renderLayers;
        double layers = Math.ceil(lt * maxLayers);
        double spacing = height / (maxLayers + 1);
        Location center = target.getLocation().add(0, 1.8 / 2.0, 0);

        for (int i = 1; i <= layers; ++i) {
            double y = (i * spacing) - r;
            double f = 1.0 - (Math.abs(y) * Math.abs(y)) / (r * r);

            for (double theta = 0.0; theta < Math.PI * 2.0; theta += Math.PI * 2.0 / 5.0) {
                double x = r * f * Math.cos(theta);
                double z = r * f * Math.sin(theta);

                AirElement.display(center.clone().add(x, y, z), 1, 0.0f, 0.0f, 0.0f, 0.0f, ThreadLocalRandom.current().nextInt(20) == 0);
            }
        }
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
        AbilityUser target = SpringContext.getInstance().getBean(AbilityUserService.class).getAbilityUser(this.target);
        if (this.started && target != null) {
            // Allow the user to bend again.
            target.getAbilityActivateConditional().remove(conditional);
        }
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    private static class SuffocatingConditional implements AbilityConditional {
        @Override
        public boolean matches(AbilityUser user, AbilityInformation information) {
            return false;
        }
    }
}
