package cz.tul.drone;


import com.esri.core.geometry.Point;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import cz.tul.drone.processing.*;
import cz.tul.drone.utils.KrovakWGS84Convertor;

import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 * Created by Jan Kozánek on 21.02.2017.
 */

@Path("/service")
public class Main {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    @GET
    @Path("/test/{x}/{y}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response test(@PathParam("x") double x, @PathParam("y") double y) throws IOException, NamingException, SQLException {
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
        Point gps = conv.convert(x, y);
        System.out.println(gps.getX());
        System.out.println(gps.getY());
        Point krovak = conv.convertBack(gps.getX(), gps.getY());
        System.out.println(Math.floor(krovak.getX()));
        System.out.println(Math.floor(krovak.getY()));
        gps = conv.convert(Math.floor(krovak.getX()), Math.floor(krovak.getY()));
        System.out.println(gps.getX());
        System.out.println(gps.getY());
        return Response.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    //[[15.071426537851465, 50.77312759310769], [15.071426537851465, 50.77312759310769], ...]
    @POST
    @Path("/multipleGpsToSJTSK")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Response multipleGpsToSJTSK(String points) {
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
        Gson gson = new Gson();
        double[][] new_points = gson.fromJson(points, double[][].class);
        double[][] converted = new double[new_points.length][2];
        for (int i=0; i < new_points.length; i++) {
            Point krovak = conv.convertBack(new_points[i][0], new_points[i][1]);
            converted[i][0] = Math.floor(krovak.getX());
            converted[i][1] = Math.floor(krovak.getY());
        }
        return Response.ok(gson.toJson(converted)).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/SJTSKToGps/{x}/{y}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response SJTSKToGps(@PathParam("x") double x, @PathParam("y") double y) {
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
        Gson gson = new Gson();
        Point krovak = new Point(x, y);
        Point gps = conv.convert(Math.floor(krovak.getX()), Math.floor(krovak.getY()));
        double[] res = new double[2];
        res[0] = gps.getX();
        res[1] = gps.getY();
        return Response.ok(gson.toJson(res)).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/gpsToSJTSK/{x}/{y}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response gpsToSJTSK(@PathParam("x") double x, @PathParam("y") double y) {
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
        Gson gson = new Gson();
        Point gps = new Point(x, y);
        Point krovak = conv.convertBack(gps.getX(), gps.getY());
        double[] res = new double[2];
        res[0] = Math.floor(krovak.getX());
        res[1] = Math.floor(krovak.getY());
        return Response.ok(gson.toJson(res)).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/getTilesMxN/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Response getTilesMxNPost(String json) throws IOException, JAXBException, NamingException, SQLException {
        JsonParser jp = new JsonParser();
        JsonElement root = jp.parse(json);
        double x = root.getAsJsonObject().get("x").getAsDouble();
        double y = root.getAsJsonObject().get("y").getAsDouble();
        int width = root.getAsJsonObject().get("width").getAsInt();
        int height = root.getAsJsonObject().get("height").getAsInt();
        if (root.getAsJsonObject().has("movementMode") && root.getAsJsonObject().has("movementPoints")) {
            String movementMode = root.getAsJsonObject().get("movementMode").getAsString();
            JsonArray movementPoints = root.getAsJsonObject().get("movementPoints").getAsJsonArray();
//            for(JsonElement je: movementPoints) {
//                System.out.println(je);
//            }
        }

        Scene scene = null;
        Point krovak = new Point(x,y);
        krovak.setX(Math.floor(krovak.getX()));
        krovak.setY(Math.floor(krovak.getY()));

        DBFactory db = new DBFactory();

        try {
            db.getCon().setAutoCommit(false);
            if (!db.isDBEmpty()) {
                ResultSet tileFromDb = db.getTileForCoords(Math.floor(krovak.getX()), Math.floor(krovak.getY()));

                if (tileFromDb.next()) {
                    krovak = new Point(tileFromDb.getDouble(1), tileFromDb.getDouble(2));
                } else {
//                    krovak = db.positionTileToGrid(krovak.getX(), krovak.getY(), 128);
                }
            }

            DMRDownloader dmr = new DMRDownloader(db,"http://ags.cuzk.cz/arcgis2/rest/services/dmr5g/ImageServer/getSamples","http://ags.cuzk.cz/arcgis/rest/services/ortofoto/MapServer/export");
            dmr.downloadDMR(Math.floor(krovak.getX()), Math.floor(krovak.getY()), width, height);

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            EntityDownloader en = new EntityDownloader(db, System.getProperty("java.io.tmpdir")+ "/osmfile"+sdf.format(timestamp)+".osm");
            en.downloadEntities(Math.floor(krovak.getX()), Math.floor(krovak.getY()), width, height);

            scene = new Scene();
            scene.makeScene(dmr.getTiles(), en.getEntities());

            db.getCon().commit();
        } finally {
            db.getCon().close();
        }

        return Response.ok(scene.getJson()).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getTilesMxN/{x}/{y}/{m}/{n}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public static Response getTilesMxN(@PathParam("x") double x, @PathParam("y") double y, @PathParam("m") int m, @PathParam("n") int n, @QueryParam("format") String format) throws IOException, JAXBException, NamingException, SQLException {
        Scene scene = null;
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
        Point krovak = conv.convertBack(x,y);
        krovak.setX(Math.floor(krovak.getX()));
        krovak.setY(Math.floor(krovak.getY()));

        DBFactory db = new DBFactory();

        try {
            db.getCon().setAutoCommit(false);
            if (!db.isDBEmpty()) {
                ResultSet tileFromDb = db.getTileForCoords(Math.floor(krovak.getX()), Math.floor(krovak.getY()));

                if (tileFromDb.next()) {
                    krovak = new Point(tileFromDb.getDouble(1), tileFromDb.getDouble(2));
                } else {
                    krovak = db.positionTileToGrid(krovak.getX(), krovak.getY(), 128);
                }
            }

            DMRDownloader dmr = new DMRDownloader(db,"http://ags.cuzk.cz/arcgis2/rest/services/dmr5g/ImageServer/getSamples","http://ags.cuzk.cz/arcgis/rest/services/ortofoto/MapServer/export");
            dmr.downloadDMR(Math.floor(krovak.getX()), Math.floor(krovak.getY()), m, n);

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            EntityDownloader en = new EntityDownloader(db, System.getProperty("java.io.tmpdir")+ "/osmfile"+sdf.format(timestamp)+".osm");
            en.downloadEntities(Math.floor(krovak.getX()), Math.floor(krovak.getY()), m, n);

            scene = new Scene();
            scene.makeScene(dmr.getTiles(), en.getEntities());

            db.getCon().commit();
        } finally {
            db.getCon().close();
        }

        if ("xml".equals(format)) {
            return Response.ok(scene).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            return Response.ok(scene.getJson()).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        }

    }

    @GET
    @Path("/getTilesAround/{x}/{y}/{direction}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public static Response getTilesAround(@PathParam("x") double x, @PathParam("y") double y, @PathParam("direction") String direction, @QueryParam("format") String format) throws IOException, JAXBException, SQLException {
        Scene scene = null;
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
        Point krovak = conv.convertBack(x,y);
        krovak.setX(Math.floor(krovak.getX()));
        krovak.setY(Math.floor(krovak.getY()));

        if (DroneDirectionEnum.isInEnum(direction, DroneDirectionEnum.class)) {
            if (Objects.equals(direction, "N")) {
                krovak = DroneDirectionEnum.moveNorth(krovak.getX(), krovak.getY(), 128);
            } else if (Objects.equals(direction, "S")) {
                krovak = DroneDirectionEnum.moveSouth(krovak.getX(), krovak.getY(), 128);
            } else if (Objects.equals(direction, "E")) {
                krovak = DroneDirectionEnum.moveEast(krovak.getX(), krovak.getY(), 128);
            } else if (Objects.equals(direction, "W")) {
                krovak = DroneDirectionEnum.moveWest(krovak.getX(), krovak.getY(), 128);
            } else if (Objects.equals(direction, "NW")) {
                krovak = DroneDirectionEnum.moveNorthWest(krovak.getX(), krovak.getY(), 128);
            } else if (Objects.equals(direction, "NE")) {
                krovak = DroneDirectionEnum.moveNorthEast(krovak.getX(), krovak.getY(), 128);
            } else if (Objects.equals(direction, "SE")) {
                krovak = DroneDirectionEnum.moveSouthEast(krovak.getX(), krovak.getY(), 128);
            } else if (Objects.equals(direction, "SW")) {
                krovak = DroneDirectionEnum.moveSouthWest(krovak.getX(), krovak.getY(), 128);
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Incorrect direction").build();
        }

        DBFactory db = new DBFactory();

        try {
            db.getCon().setAutoCommit(false);
            if (!db.isDBEmpty()) {
                ResultSet tileFromDb = db.getTileForCoords(Math.floor(krovak.getX()), Math.floor(krovak.getY()));

                if (tileFromDb.next()) {
                    krovak = new Point(tileFromDb.getDouble(1), tileFromDb.getDouble(2));
                } else {
                    krovak = db.positionTileToGrid(krovak.getX(), krovak.getY(), 128);
//                    krovak = db.positionTileToGridAccordingToEnvelope(krovak.getX(), krovak.getY(), 128);
                }
            }

            DMRDownloader dmr = new DMRDownloader(db,"http://ags.cuzk.cz/arcgis2/rest/services/dmr5g/ImageServer/getSamples","http://ags.cuzk.cz/arcgis/rest/services/ortofoto/MapServer/export");
            dmr.downloadDMR(Math.floor(krovak.getX()), Math.floor(krovak.getY()), 1);

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            EntityDownloader en = new EntityDownloader(db, System.getProperty("java.io.tmpdir")+ "/osmfile"+sdf.format(timestamp)+".osm");
            en.downloadEntities(Math.floor(krovak.getX()), Math.floor(krovak.getY()), 1);

            scene = new Scene();
            scene.makeScene(dmr.getTiles(), en.getEntities());

            db.getCon().commit();
        } finally {
            db.getCon().close();
        }

        if ("xml".equals(format)) {
            return Response.ok(scene).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            return Response.ok(scene.getJson()).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        }

    }

    @GET
    @Path("/getTilesAroundMap/{x}/{y}/{num}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public static Response getTilesAroundMap(@PathParam("x") double x, @PathParam("y") double y, @PathParam("num") int num, @QueryParam("format") String format) throws IOException, JAXBException, SQLException {
        Scene scene = null;
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
        Point krovak = conv.convertBack(x,y);
        krovak.setX(Math.floor(krovak.getX()));
        krovak.setY(Math.floor(krovak.getY()));

        DBFactory db = new DBFactory();

        try {
            db.getCon().setAutoCommit(false);
            if (!db.isDBEmpty()) {
                ResultSet tileFromDb = db.getTileForCoords(Math.floor(krovak.getX()), Math.floor(krovak.getY()));

                if (tileFromDb.next()) {
                    krovak = new Point(tileFromDb.getDouble(1), tileFromDb.getDouble(2));
                } else {
                    krovak = db.positionTileToGrid(krovak.getX(), krovak.getY(), 128);
//                    krovak = db.positionTileToGridAccordingToEnvelope(krovak.getX(), krovak.getY(), 128);
                }
            }

            DMRDownloader dmr = new DMRDownloader(db,"http://ags.cuzk.cz/arcgis2/rest/services/dmr5g/ImageServer/getSamples","http://ags.cuzk.cz/arcgis/rest/services/ortofoto/MapServer/export");
            dmr.downloadDMR(Math.floor(krovak.getX()), Math.floor(krovak.getY()), num);

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            EntityDownloader en = new EntityDownloader(db, System.getProperty("java.io.tmpdir")+ "/osmfile"+sdf.format(timestamp)+".osm");
            en.downloadEntities(Math.floor(krovak.getX()), Math.floor(krovak.getY()), num);

            scene = new Scene();
            scene.makeScene(dmr.getTiles(), en.getEntities());

            db.getCon().commit();
        } finally {
            db.getCon().close();
        }

        if ("xml".equals(format)) {
            return Response.ok(scene).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            return Response.ok(scene.getJson()).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        }

    }

}

