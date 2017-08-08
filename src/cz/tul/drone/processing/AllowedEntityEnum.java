package cz.tul.drone.processing;

/**
 * Created by Jan Kozánek on 04.03.2017.
 */

/**
 * A list of desired entities according to OSM file
 */
public enum AllowedEntityEnum {
    BUILDING,
    AMENITY,
    LANDUSE,
    HIGHWAY;

    /**
     * Gets an array of all allowed entities from Enum and appends them to a single string divided by "|"
     * @return String of allowed entities in format "entity1|entity2|...|entityN"
     */
    public static String getAppendedString() {
        AllowedEntityEnum[] allowed = AllowedEntityEnum.values();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < allowed.length; i++) {
            if (i != allowed.length-1) {
                sb.append(allowed[i]);
                sb.append("|");
            } else {
                sb.append(allowed[i]);
            }
        }

        return sb.toString();
    }
}
