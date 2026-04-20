package com.dji.sample.control.service.utils;

import com.dji.sample.control.model.enums.RcPlus2ActionOperatorEnum;
import com.dji.sample.control.model.param.RcPlus2FlyToPointActionsParam;
import com.dji.sdk.cloudapi.control.Point;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Utility methods for transforming RC Plus 2 actions to fly_to_point target.
 */
@Slf4j
public final class RcPlus2FlyToPointActionUtils {

    private static final float MIN_FLY_TO_HEIGHT = 20.0F;
    private static final float MAX_FLY_TO_HEIGHT = 500F;
    private static final double EARTH_RADIUS_M = 6_378_137D;

    /**
     * 机头左右旋转（yaw）时，在缺少真实转弯半径与地速的情况下，用固定转弯半径将一次 yaw 近似为水平圆弧，
     * 使该步同时产生经纬度位移（弦方向为航迹起点到终点的直线）。
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
     *   <li>{@code FORWARD}: move along current heading by meters</li>
     *   <li>{@code TURN_LEFT}/{@code TURN_RIGHT}: 机头左/右 yaw（度），按固定转弯半径做水平圆弧，
     *   用弦长与弦向方位更新经纬度，并更新航向</li>
     * </ul>
     * Height keeps current value and is clamped to fly_to_point valid range.</p>
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
        double heading = normalizeHeading(currentHead);
        double latitude = currentLatitude;
        double longitude = currentLongitude;
        int step = 0;
        for (RcPlus2FlyToPointActionsParam.Action action : actions) {
            step++;
            RcPlus2ActionOperatorEnum operator = RcPlus2ActionOperatorEnum.find(action.getOperator());
            float value = action.getValue();
            double beforeHeading = heading;
            double beforeLatitude = latitude;
            double beforeLongitude = longitude;
            switch (operator) {
                case FORWARD:
                    double[] next = moveForward(latitude, longitude, heading, value);
                    latitude = next[0];
                    longitude = next[1];
                    break;
                case TURN_LEFT: {
                    double chordM = chordLengthForYawArc(value, YAW_TURN_RADIUS_METERS);
                    double chordBearingDeg = normalizeHeading(beforeHeading - value / 2D);
                    double[] afterTurn = moveForward(latitude, longitude, chordBearingDeg, (float) chordM);
                    latitude = afterTurn[0];
                    longitude = afterTurn[1];
                    heading = normalizeHeading(beforeHeading - value);
                    break;
                }
                case TURN_RIGHT: {
                    double chordM = chordLengthForYawArc(value, YAW_TURN_RADIUS_METERS);
                    double chordBearingDeg = normalizeHeading(beforeHeading + value / 2D);
                    double[] afterTurn = moveForward(latitude, longitude, chordBearingDeg, (float) chordM);
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
     * 定半径水平转弯时，圆弧两端点之间的弦长（米）。θ 为转弯角（度），R 为转弯半径（米）。
     */
    private static double chordLengthForYawArc(float turnDegrees, double radiusM) {
        double halfTurnRad = Math.toRadians(turnDegrees) / 2D;
        return 2D * radiusM * Math.sin(halfTurnRad);
    }

    /**
     * Move one step on WGS84 sphere by heading and distance.
     *
     * @param latitudeDeg start latitude in degrees
     * @param longitudeDeg start longitude in degrees
     * @param headingDeg bearing in degrees, where north is 0
     * @param distanceM distance in meters
     * @return target coordinate array: [latitudeDeg, longitudeDeg]
     */
    private static double[] moveForward(double latitudeDeg, double longitudeDeg, double headingDeg, float distanceM) {
        double latRad = Math.toRadians(latitudeDeg);
        double lonRad = Math.toRadians(longitudeDeg);
        double bearingRad = Math.toRadians(headingDeg);
        double angularDistance = distanceM / EARTH_RADIUS_M;

        double targetLatRad = Math.asin(
                Math.sin(latRad) * Math.cos(angularDistance)
                        + Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(bearingRad));
        double targetLonRad = lonRad + Math.atan2(
                Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(latRad),
                Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(targetLatRad));
        double targetLonDeg = Math.toDegrees(targetLonRad);
        while (targetLonDeg > 180D) {
            targetLonDeg -= 360D;
        }
        while (targetLonDeg < -180D) {
            targetLonDeg += 360D;
        }
        return new double[]{Math.toDegrees(targetLatRad), targetLonDeg};
    }

    /**
     * Normalize float heading to range [0, 360).
     *
     * @param heading heading in degrees
     * @return normalized heading in degrees
     */
    private static double normalizeHeading(float heading) {
        return normalizeHeading((double) heading);
    }

    /**
     * Normalize heading to range [0, 360).
     *
     * @param heading heading in degrees
     * @return normalized heading in degrees
     */
    private static double normalizeHeading(double heading) {
        double normalized = heading % 360D;
        return normalized < 0D ? normalized + 360D : normalized;
    }

    /**
     * Clamp height into fly_to_point supported range.
     *
     * @param height input height
     * @return clamped height in range [2.0, 10000.0]
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
