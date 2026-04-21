package com.dji.sample.control.service.utils;

import com.dji.sample.control.model.enums.RcPlus2ActionOperatorEnum;
import com.dji.sample.control.model.param.RcPlus2FlyToPointActionsParam;
import com.dji.sdk.cloudapi.control.Point;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Utility methods for transforming RC Plus 2 actions to fly_to_point target.
 *
 * <p>Geodesic computation uses Vincenty's direct formulae on the WGS84 ellipsoid,
 * which yields sub-millimeter accuracy for distances within a few thousand kilometers.
 * This replaces the previous spherical Haversine-based approach whose relative error
 * reaches 0.3%-0.5% in mid to high latitudes due to Earth's oblateness.</p>
 */
@Slf4j
public final class RcPlus2FlyToPointActionUtils {

    private static final float MIN_FLY_TO_HEIGHT = 20.0F;
    private static final float MAX_FLY_TO_HEIGHT = 500F;

    // ---------------------------------------------------------------------
    // WGS84 ellipsoid parameters (defining constants, not derived).
    // Reference: NIMA TR8350.2, "Department of Defense World Geodetic System 1984".
    // ---------------------------------------------------------------------
    /** Semi-major axis a, in meters. */
    private static final double WGS84_A = 6_378_137.0D;
    /** Flattening f. */
    private static final double WGS84_F = 1.0D / 298.257223563D;
    /** Semi-minor axis b = a(1-f), in meters. */
    private static final double WGS84_B = WGS84_A * (1.0D - WGS84_F);

    /** Convergence tolerance for Vincenty iteration (radians). */
    private static final double VINCENTY_TOLERANCE = 1.0e-12D;
    /** Maximum iterations guard for Vincenty direct solution. */
    private static final int VINCENTY_MAX_ITERATIONS = 200;

    /**
     * Horizontal turn radius used to approximate a yaw action as a planar arc when
     * real turn radius and ground speed are unavailable. The chord between arc
     * endpoints (not the yaw itself) drives the lat/lon displacement of the step.
     */
    private static final double YAW_TURN_RADIUS_METERS = 1.0D;

    /**
     * Utility class; no instances allowed.
     */
    private RcPlus2FlyToPointActionUtils() {
    }

    /**
     * Apply a sequence of action commands to current OSD pose and calculate one target point.
     *
     * <p>Rules:
     * <ul>
     *   <li>{@code FORWARD}: move along current heading by meters (WGS84 geodesic)</li>
     *   <li>{@code TURN_LEFT}/{@code TURN_RIGHT}: yaw the aircraft nose left/right by the
     *   given degrees; the step is modeled as a horizontal circular arc of fixed radius,
     *   contributing a chord displacement (bearing equals mid-arc tangent direction) and
     *   updating heading accordingly</li>
     * </ul>
     * Height retains its current value clamped to the fly_to_point supported range.</p>
     *
     * @param actions action list to execute in order
     * @param currentHead current aircraft heading from OSD, in degrees
     * @param currentLatitude current latitude from OSD
     * @param currentLongitude current longitude from OSD
     * @param currentHeight current height from OSD
     * @return target fly_to_point point after all actions are applied
     */
    public static Point calculatePointByActions(List<RcPlus2FlyToPointActionsParam.Action> actions,
                                                Float currentHead,
                                                Float currentLatitude,
                                                Float currentLongitude,
                                                Float currentHeight) {
        // Promote all working state to double precision to avoid intermediate float loss.
        double heading = normalizeHeading(currentHead.doubleValue());
        double latitude = currentLatitude.doubleValue();
        double longitude = currentLongitude.doubleValue();
        int step = 0;
        for (RcPlus2FlyToPointActionsParam.Action action : actions) {
            step++;
            RcPlus2ActionOperatorEnum operator = RcPlus2ActionOperatorEnum.find(action.getOperator());
            double value = action.getValue();
            double beforeHeading = heading;
            double beforeLatitude = latitude;
            double beforeLongitude = longitude;
            switch (operator) {
                case FORWARD: {
                    double[] next = moveForward(latitude, longitude, heading, value);
                    latitude = next[0];
                    longitude = next[1];
                    break;
                }
                case TURN_LEFT: {
                    double chordM = chordLengthForYawArc(value, YAW_TURN_RADIUS_METERS);
                    double chordBearingDeg = normalizeHeading(beforeHeading - value / 2.0D);
                    double[] afterTurn = moveForward(latitude, longitude, chordBearingDeg, chordM);
                    latitude = afterTurn[0];
                    longitude = afterTurn[1];
                    heading = normalizeHeading(beforeHeading - value);
                    break;
                }
                case TURN_RIGHT: {
                    double chordM = chordLengthForYawArc(value, YAW_TURN_RADIUS_METERS);
                    double chordBearingDeg = normalizeHeading(beforeHeading + value / 2.0D);
                    double[] afterTurn = moveForward(latitude, longitude, chordBearingDeg, chordM);
                    latitude = afterTurn[0];
                    longitude = afterTurn[1];
                    heading = normalizeHeading(beforeHeading + value);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported action operator: " + action.getOperator());
            }
            log.info("[RC-CTRL][fly_to_point_actions] step={} operator={} value={} before={{heading={},lat={},lon={}}} after={{heading={},lat={},lon={}}}",
                    step,
                    operator,
                    value,
                    beforeHeading,
                    beforeLatitude,
                    beforeLongitude,
                    heading,
                    latitude,
                    longitude);
        }
        return new Point()
                .setLatitude((float) latitude)
                .setLongitude((float) longitude)
                .setHeight(clampHeight(currentHeight));
    }

    /**
     * Chord length (meters) between the two endpoints of a horizontal circular arc
     * defined by turn angle theta (degrees) and radius R (meters): 2R*sin(theta/2).
     */
    private static double chordLengthForYawArc(double turnDegrees, double radiusM) {
        double halfTurnRad = Math.toRadians(turnDegrees) / 2.0D;
        return 2.0D * radiusM * Math.sin(halfTurnRad);
    }

    /**
     * Compute the destination coordinate along a geodesic on the WGS84 ellipsoid,
     * given a start point, initial bearing, and ellipsoidal distance.
     *
     * <p>Implements Vincenty's direct formulae (T. Vincenty, 1975). Accuracy is
     * sub-millimeter for typical aviation distances; iteration converges within
     * a handful of steps for non-antipodal geometry.</p>
     *
     * @param latitudeDeg start latitude in degrees
     * @param longitudeDeg start longitude in degrees
     * @param headingDeg initial bearing in degrees, where north is 0 and east is 90
     * @param distanceM geodesic distance in meters
     * @return target coordinate array: [latitudeDeg, longitudeDeg]
     */
    private static double[] moveForward(double latitudeDeg, double longitudeDeg, double headingDeg, double distanceM) {
        // Degenerate case: zero distance, return input unchanged to avoid numerical drift.
        if (distanceM == 0.0D) {
            return new double[]{latitudeDeg, normalizeLongitude(longitudeDeg)};
        }

        double phi1 = Math.toRadians(latitudeDeg);
        double lambda1 = Math.toRadians(longitudeDeg);
        double alpha1 = Math.toRadians(headingDeg);

        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);

        // Reduced latitude U1 = atan((1-f) * tan(phi1)).
        double tanU1 = (1.0D - WGS84_F) * Math.tan(phi1);
        double cosU1 = 1.0D / Math.sqrt(1.0D + tanU1 * tanU1);
        double sinU1 = tanU1 * cosU1;

        // sigma1 = angular distance on the sphere from the equator to P1 along the geodesic.
        double sigma1 = Math.atan2(tanU1, cosAlpha1);

        // sin(alpha) = cos(U1) * sin(alpha1); alpha is the azimuth of the geodesic at the equator.
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1.0D - sinAlpha * sinAlpha;

        double uSq = cosSqAlpha
                * (WGS84_A * WGS84_A - WGS84_B * WGS84_B)
                / (WGS84_B * WGS84_B);
        double A = 1.0D + uSq / 16384.0D
                * (4096.0D + uSq * (-768.0D + uSq * (320.0D - 175.0D * uSq)));
        double B = uSq / 1024.0D
                * (256.0D + uSq * (-128.0D + uSq * (74.0D - 47.0D * uSq)));

        double sigma = distanceM / (WGS84_B * A);
        double sigmaPrev;
        double cos2SigmaM;
        double sinSigma;
        double cosSigma;
        double deltaSigma;

        int iterations = 0;
        do {
            cos2SigmaM = Math.cos(2.0D * sigma1 + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            deltaSigma = B * sinSigma * (cos2SigmaM + B / 4.0D
                    * (cosSigma * (-1.0D + 2.0D * cos2SigmaM * cos2SigmaM)
                    - B / 6.0D * cos2SigmaM * (-3.0D + 4.0D * sinSigma * sinSigma)
                    * (-3.0D + 4.0D * cos2SigmaM * cos2SigmaM)));
            sigmaPrev = sigma;
            sigma = distanceM / (WGS84_B * A) + deltaSigma;
        } while (Math.abs(sigma - sigmaPrev) > VINCENTY_TOLERANCE
                && ++iterations < VINCENTY_MAX_ITERATIONS);

        if (iterations >= VINCENTY_MAX_ITERATIONS) {
            log.warn("[RC-CTRL][fly_to_point_actions] Vincenty direct solution did not converge within {} iterations; lastDelta={}",
                    VINCENTY_MAX_ITERATIONS, Math.abs(sigma - sigmaPrev));
        }

        double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
        double phi2Numerator = sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1;
        double phi2Denominator = (1.0D - WGS84_F) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp);
        double phi2 = Math.atan2(phi2Numerator, phi2Denominator);

        double lambdaNumerator = sinSigma * sinAlpha1;
        double lambdaDenominator = cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1;
        double lambda = Math.atan2(lambdaNumerator, lambdaDenominator);

        double C = WGS84_F / 16.0D * cosSqAlpha * (4.0D + WGS84_F * (4.0D - 3.0D * cosSqAlpha));
        double L = lambda - (1.0D - C) * WGS84_F * sinAlpha
                * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma
                * (-1.0D + 2.0D * cos2SigmaM * cos2SigmaM)));

        double lambda2 = lambda1 + L;
        double targetLatDeg = Math.toDegrees(phi2);
        double targetLonDeg = normalizeLongitude(Math.toDegrees(lambda2));
        return new double[]{targetLatDeg, targetLonDeg};
    }

    /**
     * Normalize longitude to the range [-180, 180) using IEEE remainder semantics,
     * which is numerically more robust than iterative subtraction.
     *
     * @param longitudeDeg input longitude in degrees, may be any real value
     * @return longitude in degrees within [-180, 180)
     */
    private static double normalizeLongitude(double longitudeDeg) {
        double normalized = ((longitudeDeg + 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D;
        // Guard against the boundary case where modulo yields +180 exactly due to rounding.
        if (normalized == 180.0D) {
            return -180.0D;
        }
        return normalized;
    }

    /**
     * Normalize heading to range [0, 360).
     *
     * @param heading heading in degrees
     * @return normalized heading in degrees
     */
    private static double normalizeHeading(double heading) {
        double normalized = heading % 360.0D;
        return normalized < 0.0D ? normalized + 360.0D : normalized;
    }

    /**
     * Clamp height into fly_to_point supported range.
     *
     * @param height input height
     * @return clamped height
     */
    private static float clampHeight(float height) {
        if (height < MIN_FLY_TO_HEIGHT) {
            return MIN_FLY_TO_HEIGHT;
        }
        if (height > MAX_FLY_TO_HEIGHT) {
            return MAX_FLY_TO_HEIGHT;
        }
        return height;
    }
}