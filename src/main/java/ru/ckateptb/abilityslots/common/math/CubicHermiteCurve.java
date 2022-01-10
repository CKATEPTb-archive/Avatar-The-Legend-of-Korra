package ru.ckateptb.abilityslots.common.math;

import ru.ckateptb.tablecloth.math.Vector3d;

// Represents a curve between a start and end point.
public class CubicHermiteCurve {
    private static final int LENGTH_INTERPOLATION_COUNT = 5;

    private Vector3d startPoint;
    private Vector3d startTangent;
    private Vector3d endPoint;
    private Vector3d endTangent;
    private double chordalDistance;

    public CubicHermiteCurve(Vector3d startPoint, Vector3d startTanget, Vector3d endPoint, Vector3d endTangent) {
        this.startPoint = startPoint;
        this.startTangent = startTanget;
        this.endPoint = endPoint;
        this.endTangent = endTangent;
        // Approximate arc-length by just using chordal distance.
        this.chordalDistance = endPoint.distance(startPoint);
    }

    public Vector3d interpolate(double t) {
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

        Vector3d current = startPoint;

        for (int i = 0; i < LENGTH_INTERPOLATION_COUNT; ++i) {
            Vector3d interpolated = interpolate(i / (double) LENGTH_INTERPOLATION_COUNT);

            result += current.distance(interpolated);
            current = interpolated;
        }

        return result;
    }
}
