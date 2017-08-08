package cz.tul.drone.processing;

import com.google.gson.Gson;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Jan Kozánek on 22.02.2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"id", "type", "maxZ", "minZ", "SJTSK"})
public class Entity {

    private String id;
    private String type;
    private double maxZ = 0;
    private double minZ = 0;
    private transient ArrayList<String> references = new ArrayList<>();
    private transient double[][] WGS84;
    @XmlElement
    private double[][] SJTSK;

    public Entity() {}

    public Entity(int capacity) {
        this.WGS84 = new double[capacity][];
        this.SJTSK = new double[capacity][];
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() { return type; }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getReferences() {
        return references;
    }

    public void addReference(String reference) {
        this.references.add(reference);
    }

    public double getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(double maxZ) {
        this.maxZ = maxZ;
    }

    public double getMinZ() {
        return minZ;
    }

    public void setMinZ(double minZ) {
        this.minZ = minZ;
    }

    public double[][] getWGS84() {
        return WGS84;
    }

    public void addWGS84OfReference(int position, double[] WGS84OfReference) {
        this.WGS84[position] = WGS84OfReference;
    }

    public double[][] getSJTSK() {
        return SJTSK;
    }

    public void setSJTSK(double[][] SJTSK) {
        this.SJTSK = SJTSK;
    }

    public void addKrovakOfReference(int position, double[] KrovakOfReference) {
        this.SJTSK[position] = KrovakOfReference;
    }

    public String getSJTSKinJSON() {
        Gson gson = new Gson();
        return gson.toJson(this.getSJTSK());
    }

    private String getWGS84XandYCoordinates(double[][] WGS84) {
        StringBuilder sb = new StringBuilder();
        sb.append("");
        if (WGS84 != null) {
            for (int i = 0; i < WGS84.length; i++) {
                if (WGS84[i] == null) {
                    return "empty";
                } else {
                    sb.append("[" + "x: " + WGS84[i][0] + ", y: " + WGS84[i][1] + "], ");
                }

            }
        } else {
            sb.append("");
        }

        return sb.toString();
    }

    private String getKrovakXandYCoordinates(double[][] SJTSK) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SJTSK.length; i++) {
            if (SJTSK[i] == null) {
                return "empty";
            } else {
                sb.append("[" + "x: " + Math.floor(SJTSK[i][0]) + ", y: " + Math.floor(SJTSK[i][1]) + "], ");
            }

        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", references=" + references +
                ", maxZ=" + maxZ +
                ", WGS84=" + Arrays.toString(WGS84) +
                ", SJTSK=" + Arrays.toString(SJTSK) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entity entity = (Entity) o;

        return id.equals(entity.id);
    }
}
