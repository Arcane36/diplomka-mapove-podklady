package cz.tul.drone.utils;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeographicTransformation;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;

/**
 * @author ikopetschke
 */
public abstract class AbstractCoordinatesConvertor {

    protected GeographicTransformation gtrans, gtransBack;
    protected SpatialReference srFrom, srTo;

    public AbstractCoordinatesConvertor(WKTSEnum wktsConversion,
                                        WKIDEnum wkidConversion, WKIDEnum wkidFrom, WKIDEnum wkidTo) {
        srFrom = SpatialReference.create(wkidFrom.getId());
        srTo = SpatialReference.create(wkidTo.getId());
        gtrans = GeographicTransformation.create(wkidConversion.getId());
        gtransBack =
                GeographicTransformation.create(wkidConversion.getId(), false);
    }

    public boolean isInitialized() {
        return gtrans != null;
    }

    public Point convert(double x, double y) {
        Point p = new Point(x, y);
        return (Point) GeometryEngine.project(p, srFrom, srTo, gtrans);
    }

    public Point convertBack(double x, double y) {
        Point p = new Point(x, y);
        return (Point) GeometryEngine.project(p, srTo, srFrom, gtransBack);
    }

    public Point convert(Point point) {
        return convert(point.getX(), point.getY());
    }

    public Point convertBack(Point point) {
        return convertBack(point.getX(), point.getY());
    }

    public Point convert(long x, long y) {
        return convert((double) x, (double) y);
    }

    public Point convertBack(long x, long y) {
        return convertBack((double) x, (double) y);
    }

    public Line convertLine(double startX, double startY, double endX,
                            double endY) {
        Line l = new Line();
        l.setStart(new Point(startX, startY));
        l.setEnd(new Point(endX, endY));
        return (Line) GeometryEngine.project(l, srFrom, srTo, gtrans);
    }

    public Line convertLineBack(double startX, double startY, double
            endX, double endY) {
        Line l = new Line();
        l.setStart(new Point(startX, startY));
        l.setEnd(new Point(endX, endY));
        return (Line) GeometryEngine.project(l, srTo, srFrom,gtransBack);
    }

    public Line convertLine(long startX, long startY, long endX, long
            endY) {
        return convertLine((double) startX, (double) startY, (double)
                endX, (double) endY);
    }

    public Line convertLineBack(long startX, long startY, long endX,
                                long endY) {
        return convertLineBack((double) startX, (double) startY,
                (double) endX, (double) endY);
    }

    public Line convertLine(Point startPoint, Point endPoint) {
        return convertLine(startPoint.getX(), startPoint.getY(),
                endPoint.getX(), endPoint.getY());
    }

    public Line convertLineBack(Point startPoint, Point endPoint) {
        return convertLineBack(startPoint.getX(), startPoint.getY(),
                endPoint.getX(), endPoint.getY());
    }

    public Line convertLine(Line line) {
        return convertLine(line.getStartX(), line.getStartY(),
                line.getEndX(), line.getEndY());
    }

    public Line convertLineBack(Line line) {
        return convertLineBack(line.getStartX(), line.getStartY(),
                line.getEndX(), line.getEndY());
    }

    public Envelope convertEnvelope(double startX, double startY, double
            endX, double endY) {
        Envelope e = new Envelope(startX, startY, endX, endY);
        return (Envelope) GeometryEngine.project(e, srFrom, srTo, gtrans);
    }

    public Envelope convertEnvelopeBack(double startX, double startY,
                                        double endX, double endY) {
        Envelope e = new Envelope(startX, startY, endX, endY);
        return (Envelope) GeometryEngine.project(e, srTo, srFrom,
                gtransBack);
    }

    public Envelope convertEnvelope(long startX, long startY, long endX,
                                    long endY) {
        return convertEnvelope((double) startX, (double) startY,
                (double) endX, (double) endY);
    }

    public Envelope convertEnvelopeBack(long startX, long startY, long
            endX, long endY) {
        return convertEnvelopeBack((double) startX, (double) startY,
                (double) endX, (double) endY);
    }

    public Envelope convertEnvelope(Point startPoint, Point endPoint) {
        return convertEnvelope(startPoint.getX(), startPoint.getY(),
                endPoint.getX(), endPoint.getY());
    }

    public Envelope convertEnvelopeBack(Point startPoint, Point endPoint) {
        return convertEnvelopeBack(startPoint.getX(), startPoint.getY(),
                endPoint.getX(), endPoint.getY());
    }

    public Envelope convertEnvelope(Envelope e) {
        return (Envelope) GeometryEngine.project(e, srFrom, srTo, gtrans);
    }

    public Envelope convertEnvelopeBack(Envelope e) {
        return (Envelope) GeometryEngine.project(e, srTo, srFrom,
                gtransBack);
    }
}