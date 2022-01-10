package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.burst.BurstAbility;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBurst",
        displayName = "AirBurst",
        activationMethods = {ActivationMethod.LEFT_CLICK, ActivationMethod.FALL, ActivationMethod.SNEAK},
        category = "air",
        description = "Harness all the chaos in the air around you, allowing you to channel its energy into a multitude of AirBlast. You and only you decide how to use it, around you, along a cone or when falling",
        instruction = "Several ways to use:\n" +
                "1. Select the ability when falling from a height\n" +
                "2. Hold Sneak to accumulate air\n" +
                "2.1. Left Click to create a cone\n" +
                "2.2. Release Sneak to blast the accumulated air",
        cooldown = 3000
)
public class AirBurst extends BurstAbility {
    @ConfigField(name = "sphere.chargeTime")
    private static int sphereChargeTime = 1750;
    @ConfigField(name = "sphere.cooldownMultiplier")
    private static double sphereCooldownMultiplier = 1;

    @ConfigField(name = "cone.chargeTime")
    private static int coneChargeTime = 875;
    @ConfigField(name = "cone.cooldownMultiplier")
    private static double coneCooldownMultiplier = 0.5;

    @ConfigField(name = "fall.cooldownMultiplier")
    private static double fallCooldownMultiplier = 2;
    @ConfigField(name = "fall.threshold")
    private static int fallThreshold = 10;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private long startTime;
    private boolean released;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        this.startTime = System.currentTimeMillis();
        this.released = false;

        AirBurst burst = this;
        AbilityInstanceService abilityInstanceService = getAbilityInstanceService();
        for (AirBurst airBurst : abilityInstanceService.getAbilityUserInstances(user, getClass())) {
            if (burst.released) continue;
            burst = airBurst;
        }

        if (method == ActivationMethod.FALL) {
            if (livingEntity.getFallDistance() < fallThreshold || livingEntity instanceof Player player && player.isSneaking()) {
                return ActivateResult.NOT_ACTIVATE;
            }
            burst.releaseFall();
        } else if (method == ActivationMethod.LEFT_CLICK) {
            if (burst.isConeCharged()) burst.releaseCone();
        }

        return burst == this && !livingEntity.getEyeLocation().getBlock().isLiquid() ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (!released) {
            // This renders particles to the left if cone is charged and to the right if sphere is charged.
            // Releasing will prefer sphere over cone if it's fully charged.

            boolean coneCharged = isConeCharged();
            boolean sphereCharged = isSphereCharged();

            if (coneCharged || sphereCharged) {
                Location eyeLocation = livingEntity.getEyeLocation();
                Vector3d direction = new Vector3d(eyeLocation.getDirection());
                Location location = eyeLocation.add(direction.toBukkitVector());

                Vector3d side = direction.cross(Vector3d.PLUS_J).normalize(Vector3d.PLUS_I);
                location = location.subtract(side.multiply(0.5).toBukkitVector());
                float offset = sphereCharged ? 0.25f : 0;
                // Display air particles to the left of the player.
                AirElement.display(location, 1, offset, offset, offset);
            }
            if (livingEntity instanceof Player player && !player.isSneaking()) {
                if (sphereCharged) {
                    releaseSphere();
                } else if (coneCharged) {
                    releaseCone();
                }
                if (!this.released) {
                    return UpdateResult.REMOVE;
                }
            }

            return UpdateResult.CONTINUE;
        }

        return updateBurst() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    public boolean isSphereCharged() {
        return System.currentTimeMillis() >= startTime + sphereChargeTime;
    }

    public boolean isConeCharged() {
        return System.currentTimeMillis() >= startTime + coneChargeTime;
    }

    public void releaseSphere() {
        createBurst(user, 0.0, Math.PI, Math.toRadians(10), 0, Math.PI * 2, Math.toRadians(10), AirBlast.class);
        this.released = true;
        AbilityInformation information = getInformation();
        user.setCooldown(information, (long) (information.getCooldown() * sphereCooldownMultiplier));
    }

    public void releaseCone() {
        createCone(user, AirBlast.class);
        this.released = true;
        AbilityInformation information = getInformation();
        user.setCooldown(information, (long) (information.getCooldown() * coneCooldownMultiplier));
    }

    private void releaseFall() {
        createBurst(user, Math.toRadians(75), Math.toRadians(105), Math.toRadians(10), 0, Math.PI * 2, Math.toRadians(10), AirBlast.class);
        this.released = true;
        AbilityInformation information = getInformation();
        user.setCooldown(information, (long) (information.getCooldown() * fallCooldownMultiplier));
    }
}
