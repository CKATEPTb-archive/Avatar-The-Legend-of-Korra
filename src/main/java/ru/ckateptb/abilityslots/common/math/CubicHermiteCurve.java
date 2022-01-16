package ru.ckateptb.abilityslots.common.math;

import ru.ckateptb.tablecloth.math.ImmutableVector;

// Represents a curve between a start and end point.
public class CubicHermiteCurve {
    private static final int LENGTH_INTERPOLATION_COUNT = 5;

    private final ImmutableVector startPoint;
    private final ImmutableVector startTangent;
    private final ImmutableVector endPoint;
    private final ImmutableVector endTangent;
    private final double chordalDistance;

    public CubicHermiteCurve(ImmutableVector startPoint, ImmutableVector startTanget, ImmutableVector endPoint, ImmutableVector endTangent) {
        this.startPoint = startPoint;
        this.startTangent = startTanget;
        this.endPoint = endPoint;
        this.endTangent = endTangent;
        // Approximate arc-length by just using chordal distance.
        this.chordalDistance = endPoint.distance(startPoint);
    }

    public ImmutableVector interpolate(double t) {
        t = Math.max(0.0, Math.min(t, 1.0));

        double startPointT = (2.0 * t * t * t) - (3.0 * t * t) + 1.0;
        double startTangentT = (t * t * t) - (2.0 * t * t) + t;
        double endPointT = (-2.0 * t * t * t) + (3.0 * t * t);
        double endTangentT = (t * t * t) - (t * t);

        return startPoint.multiply(startPointT)
                .add(startTangent.multiply(startTangentT))
                .add(endPoint.multiply(endPointT))
                .add(endTangent.multiply(endTangentT));
    }

    // Approximate length by summing length of n interpolated lines.
    public double getLength() {
        double result = 0.0;

        ImmutableVector current = startPoint;

        for (int i = 0; i < LENGTH_INTERPOLATION_COUNT; ++i) {
            ImmutableVector interpolated = interpolate(i / (double) LENGTH_INTERPOLATION_COUNT);

            result += current.distance(interpolated);
            current = interpolated;
        }

        return result;
    }
}
