package ru.ckateptb.abilityslots.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.collider.RayCollider;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.math.Rotation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VectorUtils {
    private static final double ANGLE_STEP = Math.toRadians(10);
    private static final double ANGLE = Math.toRadians(30);
    private static final double FALL_MIN_ANGLE = Math.toRadians(60);
    private static final double FALL_MAX_ANGLE = Math.toRadians(105);

    /**
     * Create an arc by combining {@link #rotate(ImmutableVector, Rotation, int)} and {@link #rotateInverse(ImmutableVector, Rotation, int)}.
     * Amount of rays will be rounded up to the nearest odd number. Minimum value is 3.
     *
     * @param start the starting point
     * @param axis  the axis around which to rotate
     * @param angle the rotation angle
     * @param rays  the amount of vectors to return, must be an odd number, minimum 3
     * @return a list comprising of all the directions for this arc
     * @see #rotateInverse(ImmutableVector, Rotation, int)
     */
    public static Collection<ImmutableVector> createArc(ImmutableVector start, ImmutableVector axis, double angle, int rays) {
        Rotation rotation = new Rotation(axis, angle);
        rays = Math.max(3, rays);
        if (rays % 2 == 0) {
            rays++;
        }
        int half = (rays - 1) / 2;
        Collection<ImmutableVector> arc = new ArrayList<>(rays);
        arc.add(start);
        arc.addAll(rotate(start, rotation, half));
        arc.addAll(rotateInverse(start, rotation, half));
        return arc;
    }

    public static Collection<ImmutableVector> circle(ImmutableVector start, ImmutableVector axis, int times) {
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
     * @see #rotateInverse(ImmutableVector, Rotation, int)
     */
    public static Collection<ImmutableVector> rotate(ImmutableVector start, ImmutableVector axis, double angle, int times) {
        return rotate(start, new Rotation(axis, angle), times);
    }

    private static Collection<ImmutableVector> rotate(ImmutableVector start, Rotation rotation, int times) {
        Collection<ImmutableVector> arc = new ArrayList<>();
        double[] vector = start.toArray();
        for (int i = 0; i < times; i++) {
            rotation.applyTo(vector, vector);
            arc.add(new ImmutableVector(vector));
        }
        return arc;
    }


    public static ImmutableVector rotateYaw(ImmutableVector vector, double rads) {
        return rotate(vector, ImmutableVector.PLUS_J, rads);
    }

    public static ImmutableVector rotatePitch(ImmutableVector vector, double rads) {
        ImmutableVector axis = vector.crossProduct(ImmutableVector.PLUS_J).normalize(ImmutableVector.PLUS_I);
        return rotate(vector, axis, rads);
    }

    public static ImmutableVector rotate(ImmutableVector vector, ImmutableVector axis, double rads) {
        ImmutableVector a = vector.multiply(Math.cos(rads));
        ImmutableVector b = axis.crossProduct(vector).multiply(Math.sin(rads));
        ImmutableVector c = axis.multiply(axis.dot(vector)).multiply(1 - Math.cos(rads));
        return a.add(b).add(c);
    }

    /**
     * Inversely repeat a rotation on a specific vector.
     *
     * @see #rotate(ImmutableVector, Rotation, int)
     */
    public static Collection<ImmutableVector> rotateInverse(ImmutableVector start, ImmutableVector axis, double angle, int times) {
        return rotateInverse(start, new Rotation(axis, angle), times);
    }

    private static Collection<ImmutableVector> rotateInverse(ImmutableVector start, Rotation rotation, int times) {
        Collection<ImmutableVector> arc = new ArrayList<>();
        double[] vector = start.toArray();
        for (int i = 0; i < times; i++) {
            rotation.applyInverseTo(vector, vector);
            arc.add(new ImmutableVector(vector));
        }
        return arc;
    }

    /**
     * Get an orthogonal vector.
     */
    public static ImmutableVector orthogonal(ImmutableVector axis, double radians, double length) {
        double[] arr = {axis.getY(), -axis.getX(), 0};
        Rotation rotation = new Rotation(axis, radians);
        return rotation.applyTo(new ImmutableVector(arr).normalize().multiply(length));
    }

    /**
     * Rotate a vector around the X axis.
     *
     * @param v   the vector to rotate
     * @param cos the rotation's cosine
     * @param sin the rotation's sine
     * @return the resulting vector
     * @see #rotateAroundAxisY(ImmutableVector, double, double)
     * @see #rotateAroundAxisZ(ImmutableVector, double, double)
     */
    public static ImmutableVector rotateAroundAxisX(ImmutableVector v, double cos, double sin) {
        return new ImmutableVector(v.getX(), v.getY() * cos - v.getZ() * sin, v.getY() * sin + v.getZ() * cos);
    }

    /**
     * Rotate a vector around the Y axis.
     *
     * @param v   the vector to rotate
     * @param cos the rotation's cosine
     * @param sin the rotation's sine
     * @return the resulting vector
     * @see #rotateAroundAxisX(ImmutableVector, double, double)
     * @see #rotateAroundAxisZ(ImmutableVector, double, double)
     */
    public static ImmutableVector rotateAroundAxisY(ImmutableVector v, double cos, double sin) {
        return new ImmutableVector(v.getX() * cos + v.getZ() * sin, v.getY(), v.getX() * -sin + v.getZ() * cos);
    }

    /**
     * Rotate a vector around the Z axis.
     *
     * @param v   the vector to rotate
     * @param cos the rotation's cosine
     * @param sin the rotation's sine
     * @return the resulting vector
     * @see #rotateAroundAxisX(ImmutableVector, double, double)
     * @see #rotateAroundAxisY(ImmutableVector, double, double)
     */
    public static ImmutableVector rotateAroundAxisZ(ImmutableVector v, double cos, double sin) {
        return new ImmutableVector(v.getX() * cos - v.getY() * sin, v.getX() * sin + v.getY() * cos, v.getZ());
    }

    /**
     * Decompose diagonal vectors into their cardinal components so they can be checked individually.
     * This is helpful for resolving collisions when moving blocks diagonally and need to consider all block faces.
     *
     * @param origin    the point of origin
     * @param direction the direction to check
     * @return a collection of normalized vectors corresponding to cardinal block faces
     */
    public static Collection<ImmutableVector> decomposeDiagonals(ImmutableVector origin, ImmutableVector direction) {
        double[] o = origin.toArray();
        double[] d = direction.toArray();
        Collection<ImmutableVector> possibleCollisions = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            int a = (int) Math.floor(o[i] + d[i]);
            int b = (int) Math.floor(o[i]);
            int delta = Math.min(1, Math.max(-1, a - b));
            if (delta != 0) {
                double[] v = new double[]{0, 0, 0};
                v[i] = delta;
                possibleCollisions.add(new ImmutableVector(v));
            }
        }
        if (possibleCollisions.isEmpty()) {
            return List.of(ImmutableVector.ZERO);
        }
        return possibleCollisions;
    }

    public static ImmutableVector gaussianOffset(ImmutableVector target, double offset) {
        return gaussianOffset(target, offset, offset, offset);
    }

    public static ImmutableVector gaussianOffset(ImmutableVector target, double offsetX, double offsetY, double offsetZ) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double[] v = {r.nextGaussian() * offsetX, r.nextGaussian() * offsetY, r.nextGaussian() * offsetZ};
        return target.add(new ImmutableVector(v));
    }

    public static Collection<RayCollider> cone(AbilityUser user, double range) {
        return createBurst(user, range, ANGLE_STEP, ANGLE);
    }

    public static Collection<RayCollider> sphere(AbilityUser user, double range) {
        return createBurst(user, range, ANGLE_STEP, 0);
    }

    public static Collection<RayCollider> fall(AbilityUser user, double range) {
        return createBurst(user, range, ANGLE_STEP, -1);
    }

    // Negative angle for fall burst
    public static Collection<RayCollider> createBurst(AbilityUser user, double range, double angleStep, double angle) {
        ImmutableVector center = user.getCenterLocation();
        ImmutableVector userDIr = user.getDirection();
        Collection<RayCollider> rays = new ArrayList<>();
        double epsilon = 0.001; // Needed for accuracy
        for (double theta = 0; theta < Math.PI - epsilon; theta += angleStep) {
            double z = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            for (double phi = 0; phi < 2 * Math.PI - epsilon; phi += angleStep) {
                double x = Math.cos(phi) * sinTheta;
                double y = Math.sin(phi) * sinTheta;
                ImmutableVector direction = new ImmutableVector(x, y, z);
                if (angle > 0 && direction.angle(userDIr) > angle) {
                    continue;
                }
                if (angle < 0) {
                    double vectorAngle = direction.angle(ImmutableVector.PLUS_J);
                    if (vectorAngle < FALL_MIN_ANGLE || vectorAngle > FALL_MAX_ANGLE) {
                        continue;
                    }
                }
                rays.add(new RayCollider(user.getWorld(), center, direction.multiply(range), range, 0));
            }
        }
        return rays;
    }
}
