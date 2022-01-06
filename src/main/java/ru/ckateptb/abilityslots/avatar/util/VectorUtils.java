package ru.ckateptb.abilityslots.avatar.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.collider.Ray;
import ru.ckateptb.tablecloth.math.FastMath;
import ru.ckateptb.tablecloth.math.Rotation;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.math.Vector3i;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VectorUtils {
    private static final double ANGLE_STEP = Math.toRadians(10);
    private static final double ANGLE = Math.toRadians(30);
    private static final double FALL_MIN_ANGLE = Math.toRadians(60);
    private static final double FALL_MAX_ANGLE = Math.toRadians(105);

    /**
     * Create an arc by combining {@link #rotate(Vector3d, Rotation, int)} and {@link #rotateInverse(Vector3d, Rotation, int)}.
     * Amount of rays will be rounded up to the nearest odd number. Minimum value is 3.
     *
     * @param start the starting point
     * @param axis  the axis around which to rotate
     * @param angle the rotation angle
     * @param rays  the amount of vectors to return, must be an odd number, minimum 3
     * @return a list comprising of all the directions for this arc
     * @see #rotateInverse(Vector3d, Rotation, int)
     */
    public static Collection<Vector3d> createArc(Vector3d start, Vector3d axis, double angle, int rays) {
        Rotation rotation = new Rotation(axis, angle);
        rays = Math.max(3, rays);
        if (rays % 2 == 0) {
            rays++;
        }
        int half = (rays - 1) / 2;
        Collection<Vector3d> arc = new ArrayList<>(rays);
        arc.add(start);
        arc.addAll(rotate(start, rotation, half));
        arc.addAll(rotateInverse(start, rotation, half));
        return arc;
    }

    public static Collection<Vector3d> circle(Vector3d start, Vector3d axis, int times) {
        double angle = 2 * Math.PI / times;
        return rotate(start, axis, angle, times);
    }

    /**
     * Repeat a rotation on a specific vector.
     *
     * @param start the starting point
     * @param axis  the axis around which to rotate
     * @param angle the rotation angle
     * @param times the amount of times to repeat the rotation
     * @return a list comprising of all the directions for this arc
     * @see #rotateInverse(Vector3d, Rotation, int)
     */
    public static Collection<Vector3d> rotate(Vector3d start, Vector3d axis, double angle, int times) {
        return rotate(start, new Rotation(axis, angle), times);
    }

    private static Collection<Vector3d> rotate(Vector3d start, Rotation rotation, int times) {
        Collection<Vector3d> arc = new ArrayList<>();
        double[] vector = start.toArray();
        for (int i = 0; i < times; i++) {
            rotation.applyTo(vector, vector);
            arc.add(new Vector3d(vector));
        }
        return arc;
    }

    public static Vector3d rotatePitch(Vector3d vector, double rads) {
        Vector3d axis = vector.cross(Vector3d.PLUS_J);
        return rotate(vector, axis, rads);
    }

    public static Vector3d rotate(Vector3d vector, Vector3d axis, double rads) {
        Vector3d a = vector.multiply(Math.cos(rads));
        Vector3d b = axis.cross(vector).multiply(Math.sin(rads));
        Vector3d c = axis.multiply(axis.dot(vector)).multiply(1 - Math.cos(rads));
        return a.add(b).add(c);
    }

    /**
     * Inversely repeat a rotation on a specific vector.
     *
     * @see #rotate(Vector3d, Rotation, int)
     */
    public static Collection<Vector3d> rotateInverse(Vector3d start, Vector3d axis, double angle, int times) {
        return rotateInverse(start, new Rotation(axis, angle), times);
    }

    private static Collection<Vector3d> rotateInverse(Vector3d start, Rotation rotation, int times) {
        Collection<Vector3d> arc = new ArrayList<>();
        double[] vector = start.toArray();
        for (int i = 0; i < times; i++) {
            rotation.applyInverseTo(vector, vector);
            arc.add(new Vector3d(vector));
        }
        return arc;
    }

    /**
     * Get an orthogonal vector.
     */
    public static Vector3d orthogonal(Vector3d axis, double radians, double length) {
        double[] arr = {axis.getY(), -axis.getX(), 0};
        Rotation rotation = new Rotation(axis, radians);
        return rotation.applyTo(new Vector3d(arr).normalize().multiply(length));
    }

    /**
     * Rotate a vector around the X axis.
     *
     * @param v   the vector to rotate
     * @param cos the rotation's cosine
     * @param sin the rotation's sine
     * @return the resulting vector
     * @see #rotateAroundAxisY(Vector3d, double, double)
     * @see #rotateAroundAxisZ(Vector3d, double, double)
     */
    public static Vector3d rotateAroundAxisX(Vector3d v, double cos, double sin) {
        return new Vector3d(v.getX(), v.getY() * cos - v.getZ() * sin, v.getY() * sin + v.getZ() * cos);
    }

    /**
     * Rotate a vector around the Y axis.
     *
     * @param v   the vector to rotate
     * @param cos the rotation's cosine
     * @param sin the rotation's sine
     * @return the resulting vector
     * @see #rotateAroundAxisX(Vector3d, double, double)
     * @see #rotateAroundAxisZ(Vector3d, double, double)
     */
    public static Vector3d rotateAroundAxisY(Vector3d v, double cos, double sin) {
        return new Vector3d(v.getX() * cos + v.getZ() * sin, v.getY(), v.getX() * -sin + v.getZ() * cos);
    }

    /**
     * Rotate a vector around the Z axis.
     *
     * @param v   the vector to rotate
     * @param cos the rotation's cosine
     * @param sin the rotation's sine
     * @return the resulting vector
     * @see #rotateAroundAxisX(Vector3d, double, double)
     * @see #rotateAroundAxisY(Vector3d, double, double)
     */
    public static Vector3d rotateAroundAxisZ(Vector3d v, double cos, double sin) {
        return new Vector3d(v.getX() * cos - v.getY() * sin, v.getX() * sin + v.getY() * cos, v.getZ());
    }

    /**
     * Decompose diagonal vectors into their cardinal components so they can be checked individually.
     * This is helpful for resolving collisions when moving blocks diagonally and need to consider all block faces.
     *
     * @param origin    the point of origin
     * @param direction the direction to check
     * @return a collection of normalized vectors corresponding to cardinal block faces
     */
    public static Collection<Vector3i> decomposeDiagonals(Vector3d origin, Vector3d direction) {
        double[] o = origin.toArray();
        double[] d = direction.toArray();
        Collection<Vector3i> possibleCollisions = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            int a = FastMath.floor(o[i] + d[i]);
            int b = FastMath.floor(o[i]);
            int delta = Math.min(1, Math.max(-1, a - b));
            if (delta != 0) {
                int[] v = new int[]{0, 0, 0};
                v[i] = delta;
                possibleCollisions.add(new Vector3i(v));
            }
        }
        if (possibleCollisions.isEmpty()) {
            return List.of(Vector3i.ZERO);
        }
        return possibleCollisions;
    }

    public static Vector3d gaussianOffset(Vector3d target, double offset) {
        return gaussianOffset(target, offset, offset, offset);
    }

    public static Vector3d gaussianOffset(Vector3d target, double offsetX, double offsetY, double offsetZ) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double[] v = {r.nextGaussian() * offsetX, r.nextGaussian() * offsetY, r.nextGaussian() * offsetZ};
        return target.add(new Vector3d(v));
    }

    public static Collection<Ray> cone(AbilityUser user, double range) {
        return createBurst(user, range, ANGLE_STEP, ANGLE);
    }

    public static Collection<Ray> sphere(AbilityUser user, double range) {
        return createBurst(user, range, ANGLE_STEP, 0);
    }

    public static Collection<Ray> fall(AbilityUser user, double range) {
        return createBurst(user, range, ANGLE_STEP, -1);
    }

    // Negative angle for fall burst
    public static Collection<Ray> createBurst(AbilityUser user, double range, double angleStep, double angle) {
        LivingEntity entity = user.getEntity();
        Vector3d center = WorldUtils.getEntityCenter(entity);
        Vector3d userDIr = new Vector3d(entity.getEyeLocation().getDirection());
        Collection<Ray> rays = new ArrayList<>();
        double epsilon = 0.001; // Needed for accuracy
        for (double theta = 0; theta < Math.PI - epsilon; theta += angleStep) {
            double z = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            for (double phi = 0; phi < 2 * Math.PI - epsilon; phi += angleStep) {
                double x = Math.cos(phi) * sinTheta;
                double y = Math.sin(phi) * sinTheta;
                Vector3d direction = new Vector3d(x, y, z);
                if (angle > 0 && direction.angle(userDIr) > angle) {
                    continue;
                }
                if (angle < 0) {
                    double vectorAngle = direction.angle(Vector3d.PLUS_J);
                    if (vectorAngle < FALL_MIN_ANGLE || vectorAngle > FALL_MAX_ANGLE) {
                        continue;
                    }
                }
                rays.add(new Ray(center, direction.multiply(range)));
            }
        }
        return rays;
    }
}
