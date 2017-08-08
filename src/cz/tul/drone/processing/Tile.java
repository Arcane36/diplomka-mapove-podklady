package cz.tul.drone.processing;

import com.google.gson.Gson;

import javax.xml.bind.annotation.*;
import java.util.Arrays;

/**
 * Author: Ondrej Kubicek
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"x", "y", "zMin", "zMax", "size", "step", "texture", "dataDMR"})
public class Tile {
    @XmlElement
    private double x;
    @XmlElement
    private double y;
    private float zMin = Float.MAX_VALUE;
    private float zMax = Float.MIN_VALUE;
    private int size;
    private int step;
    private String texture = "";
    private float[][] dataDMR;

    public Tile() {}

    public Tile(double x, double y, int size, int step) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.step = step;
        this.dataDMR = new float[size/2 + 1][size/2 + 1];
    }

    public Tile(double x, double y) {
        this.x = x;
        this.y = y;
        this.size = 128;
        this.step = 2;
        this.dataDMR = new float[65][65];
    }

    public double getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(long y) {
        this.y = y;
    }

    public float getzMin() {
        return zMin;
    }

    public void setzMin(float zMin) {
        this.zMin = zMin;
    }

    public float getzMax() {
        return zMax;
    }

    public void setzMax(float zMax) {
        this.zMax = zMax;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public float[][] getDataDMR() {
        return dataDMR;
    }

    public String getDataDMRAsJSON() {
        Gson gson = new Gson();
        return gson.toJson(this.getDataDMR());
    }

    public void setDataDMR(float[][] dataDMR) {
        this.dataDMR = dataDMR;
    }

    public void setDataDMRItem(float value, int row, int col) {
        this.dataDMR[row][col] = value;
    }

    @Override
    public String toString() {
        return "Tile{" +
                "x=" + x +
                ", y=" + y +
                ", zMin=" + zMin +
                ", zMax=" + zMax +
                ", size=" + size +
                ", step=" + step +
                ", texture='" + texture + '\'' +
                ", dataDMR=" + Arrays.toString(dataDMR) +
                '}';
    }
}
