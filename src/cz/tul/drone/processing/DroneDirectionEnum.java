package cz.tul.drone.processing;

import com.esri.core.geometry.Point;

import java.util.Objects;

/**
 * Created by Jan Kozánek on 19.04.2017.
 */
public enum DroneDirectionEnum {
    N,
    S,
    E,
    W,
    NW,
    NE,
    SW,
    SE;


    public static <E extends Enum<E>> boolean isInEnum(String value, Class<E> enumClass) {
        for (E e : enumClass.getEnumConstants()) {
            if(e.name().equals(value)) { return true; }
        }
        return false;
    }

    public static Point moveNorth(double x, double y, int tileSize) {
        return new Point(x, y + tileSize);
    }

    public static Point moveSouth(double x, double y, int tileSize) {
        return new Point(x, y - tileSize);
    }

    public static Point moveEast(double x, double y, int tileSize) {
        return new Point(x + tileSize, y);
    }

    public static Point moveWest(double x, double y, int tileSize) {
        return new Point(x - tileSize, y);
    }

    public static Point moveNorthWest(double x, double y, int tileSize) {
        return new Point(x - tileSize, y + tileSize);
    }

    public static Point moveNorthEast(double x, double y, int tileSize) {
        return new Point(x + tileSize, y + tileSize);
    }

    public static Point moveSouthWest(double x, double y, int tileSize) {
        return new Point(x - tileSize, y - tileSize);
    }

    public static Point moveSouthEast(double x, double y, int tileSize) {
        return new Point(x + tileSize, y - tileSize);
    }

}
