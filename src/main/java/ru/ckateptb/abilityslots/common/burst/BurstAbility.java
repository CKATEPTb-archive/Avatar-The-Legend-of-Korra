package ru.ckateptb.abilityslots.common.burst;


import org.bukkit.Location;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.common.BurstableAbility;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.math.Vector3d;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public abstract class BurstAbility implements Ability {
    protected List<BurstableAbility> blasts = new ArrayList<>();

    protected void createBurst(AbilityUser user, double thetaMin, double thetaMax, double thetaStep, double phiMin, double phiMax, double phiStep, Class<? extends BurstableAbility> type) {
        for (double theta = thetaMin; theta < thetaMax; theta += thetaStep) {
            for (double phi = phiMin; phi < phiMax; phi += phiStep) {
                double x = Math.cos(phi) * Math.sin(theta);
                double y = Math.cos(phi) * Math.cos(theta);
                double z = Math.sin(phi);

                Vector3d direction = new Vector3d(x, y, z);

                BurstableAbility blast;
                try {
                    blast = type.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                    return;
                }

                blast.initialize(user, user.getEntity().getEyeLocation().add(direction.toBukkitVector()), direction);
                blasts.add(blast);
            }
        }
    }

    protected void createCone(AbilityUser user, Class<? extends BurstableAbility> type) {
        Location eyeLocation = user.getEntity().getEyeLocation();
        for (double theta = 0.0; theta < Math.PI; theta += Math.toRadians(10)) {
            for (double phi = 0.0; phi < Math.PI * 2; phi += Math.toRadians(10)) {
                double x = Math.cos(phi) * Math.sin(theta);
                double y = Math.cos(phi) * Math.cos(theta);
                double z = Math.sin(phi);

                Vector3d direction = new Vector3d(x, y, z);

                if (direction.angle(new Vector3d(eyeLocation.getDirection())) > Math.toRadians(30)) {
                    continue;
                }

                BurstableAbility blast;
                try {
                    blast = type.getConstructor().newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    return;
                }

                blast.initialize(user, eyeLocation.clone().add(direction.toBukkitVector()), direction);
                blasts.add(blast);
            }
        }
    }

    // Return false if all blasts are finished.
    protected boolean updateBurst() {
        blasts.removeIf(blast -> blast.update() != UpdateResult.CONTINUE);
        return !blasts.isEmpty();
    }
}
