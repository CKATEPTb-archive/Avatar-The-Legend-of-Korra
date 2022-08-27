package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.Objects;

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
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.entity.AbilityTargetLiving;
import ru.ckateptb.abilityslots.predicate.AbilityConditional;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.abilityslots.service.AbilityUserService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.ioc.IoC;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Suffocate",
        displayName = "Suffocate",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "The ability requires high concentration, but its power is overwhelming! Targets affected by this ability begin to choke, which is why they lose concentration and the ability to use abilities. All they have to do is run!",
        instruction = "Hold Sneak",
        cooldown = 5000,
        cost = 30
)
public class Suffocate extends Ability {
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
    @ConfigField
    private static long energyCostInterval = 1000;

    private LivingEntity target;
    private long startTime;
    private long nextDamageTime;
    private long nextSlowTime;
    private long nextBlindTime;
    private boolean started;
    private RemovalConditional removal;
    private AbilityConditional conditional;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.setUser(user);
        this.startTime = System.currentTimeMillis();
        this.started = false;

        this.target = user.findLivingEntity(selectRange, selectScale, true, entity -> entity != livingEntity);

        if (target == null || !user.canUse(target.getLocation())) {
            return ActivateResult.NOT_ACTIVATE;
        }

        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .costInterval(energyCostInterval)
                .range(() -> livingEntity.getLocation(), () -> target.getLocation(), range)
                .sneaking(true)
                .slot()
                .custom((user, ability) -> !Objects.equals(user.getWorld(), target.getWorld()))
                .build();

        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        long time = System.currentTimeMillis();
        if (this.removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        if (time < this.startTime + chargeTime) return UpdateResult.CONTINUE;
        if (!user.canUse(target.getLocation())) return UpdateResult.REMOVE;

        if (!this.started) {
            this.started = true;
            this.nextDamageTime = startTime + damageDelay;
            this.nextSlowTime = startTime + slowDelay;
            this.nextBlindTime = startTime + blindDelay;

            AbilityUser target = IoC.get(AbilityUserService.class).getAbilityUser(this.target);
            if (target != null) {
                // Prevent the user from bending.
                conditional = (user, ability) -> false;
                target.addAbilityActivateConditional(conditional);
            }
        }

        if (requireConstantAim && user.findLivingEntity(selectRange, constantAimRadius, true, entity -> entity == target) == null) {
            return UpdateResult.REMOVE;
        }

        handleEffects();
        render();

        return UpdateResult.CONTINUE;
    }

    private void handleEffects() {
        long time = System.currentTimeMillis();
        AbilityTargetLiving target = AbilityTarget.of(this.target);
        if (damageAmount > 0 && time > this.nextDamageTime) {
            target.damage(damageAmount, this);
            this.nextDamageTime = time + damageInterval;
        }

        if (slowAmplifier > 0 && time >= this.nextSlowTime) {
            target.removePotionEffect(this, PotionEffectType.SLOW);
            target.addPotionEffect(this, new PotionEffect(PotionEffectType.SLOW, slowInterval / 50, slowAmplifier - 1, true, false));
            this.nextSlowTime = time + slowInterval;
        }

        if (blindAmplifier > 0 && time >= this.nextBlindTime) {
            target.removePotionEffect(this, PotionEffectType.BLINDNESS);
            target.addPotionEffect(this, new PotionEffect(PotionEffectType.BLINDNESS, slowInterval / 50, slowAmplifier - 1, true, false));
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
        ImmutableVector center = AbilityTarget.of(target).getCenterLocation();

        for (int i = 1; i <= layers; ++i) {
            double y = (i * spacing) - r;
            double f = 1.0 - (Math.abs(y) * Math.abs(y)) / (r * r);

            for (double theta = 0.0; theta < Math.PI * 2.0; theta += Math.PI * 2.0 / 5.0) {
                double x = r * f * Math.cos(theta);
                double z = r * f * Math.sin(theta);
                AirElement.display(center.add(x, y, z).toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
            }
        }
        AirElement.sound(target.getLocation());
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
        AbilityUser target = IoC.get(AbilityUserService.class).getAbilityUser(this.target);
        if (this.started && target != null) {
            // Allow the user to bend again.
            target.removeAbilityActivateConditional(conditional);
        }
    }
}
