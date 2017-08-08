package cz.tul.drone.processing;

import com.google.gson.*;
import jcifs.util.Base64;
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Author: Ondrej Kubicek
 */
public class DMRDownloader {

    private String DMRServiceURL, textureServiceURL;
    private int tileSize = 128;
    private Tile[] tiles;
    private DBFactory db = null;

    /**
     * Sets up downloader.
     * @param db Instance of a database to which a generated Tile will be saved to
     * @param DMRServiceURL URL of ImageServer that offers height data of surface (ie. http://ags.cuzk.cz/arcgis/rest/services/dmr5g/ImageServer/getSamples)
     * @param textureServiceURL URL of MapServer that offers map textures (ie. http://ags.cuzk.cz/arcgis/rest/services/ortofoto/MapServer/export)
     */
    public DMRDownloader(DBFactory db, String DMRServiceURL, String textureServiceURL) {
        if (DMRServiceURL.endsWith("/")) {
            DMRServiceURL = DMRServiceURL.substring(0, DMRServiceURL.length() - 1);
        }
        if (textureServiceURL.endsWith("/")) {
            textureServiceURL = textureServiceURL.substring(0, DMRServiceURL.length() - 1);
        }

        this.db = db;
        this.DMRServiceURL = DMRServiceURL;
        this.textureServiceURL = textureServiceURL;
    }

    /**
     * Downloads height data and textures.
     * Area start point is defined by point's x and y coordinates.
     * Area is constructed from a given x, y to all directions around the tile.
     * @param x area start x coordinate
     * @param y area start y coordinate
     * @param tileNumberAround number of tiles to make to all directions
     * @throws IOException
     * @throws RuntimeException
     */
    public void downloadDMR(double x, double y, int tileNumberAround) throws IOException, RuntimeException {
        downloadDMR(x - (tileNumberAround * tileSize), y + (tileNumberAround * tileSize), (2*tileNumberAround + 1), (2*tileNumberAround + 1));
    }

    /**
     * Downloads height data and textures for selected area.
     * Area start point is defined by point's x and y coordinates. Number of tiles is defined by width and height parameters.
     * Area is constructed from a given x, y to bottom-right direction.
     * @param x area start x coordinate
     * @param y area start y coordinate
     * @param width number of tiles horizontally
     * @param height number of tiles vertically
     * @throws IOException
     * @throws RuntimeException
     */
    public void downloadDMR(double x, double y,  int width, int height) throws IOException, RuntimeException {
        tiles = new Tile[width*height];
        Tile tile = new Tile(x, y);
        tile.setSize(tileSize);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ResultSet tileFromDB = db.getTileFromDB(x + (j*tileSize), y - (i*tileSize));
                try {
                    if (!tileFromDB.next()) {
                        tiles[i*width + j] = makeDMRTile(x + (j*tileSize), y - (i*tileSize));
                        db.saveTileToDB(tiles[i*width + j]);
                    } else {
                        do {
                            Gson gson = new Gson();
                            tile = new Tile((long) tileFromDB.getDouble(1), (long) tileFromDB.getDouble(2));
                            tile.setzMin(tileFromDB.getFloat(3));
                            tile.setzMax(tileFromDB.getFloat(4));
                            tile.setSize(this.tileSize);
                            tile.setTexture(tileFromDB.getString(7));
                            tile.setDataDMR(gson.fromJson(tileFromDB.getString(8), float[][].class));
                            tiles[i*width + j] = tile;
                        } while(tileFromDB.next());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Makes request to texture service and performs texture download for given tile. Texture is saved as Base64 String.
     * @param tile actual tile that is processed
     * @param exportSize size of exported image
     * @throws IOException
     * @throws RuntimeException
     */
    private void downloadTexture(Tile tile, int exportSize) throws IOException, RuntimeException {
        URL url = new URL(textureServiceURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String geometry = "bbox=" + tile.getX() + "," + tile.getY() + "," + (tile.getX() + tile.getSize()) + "," + (tile.getY() - tile.getSize())+ "&format=png&transparent=false&size=" + exportSize + "," + exportSize + "&f=image";

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");

        OutputStream os = connection.getOutputStream();
        os.write(geometry.getBytes());
        os.flush();

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Chyba při komunikaci s REST službou " + url.toString() + " => Chybový kód: "
                    + connection.getResponseCode());
        }

        byte[] imageBytes = IOUtils.readFully(connection.getInputStream(), -1, false);

        tile.setTexture(Base64.encode(imageBytes));

        connection.disconnect();
    }

    /**
     * Makes request to ImageServer service that offers height information and performs texture download for given tile.
     * Sets height data to two dimensional array for given tile. Also computes minimal and maximal height for given tile.
     * @param geometry geometry defining current area
     * @param tile actual tile that is processed
     * @throws IOException
     * @throws RuntimeException
     */
    private void downloadTileData(String geometry, Tile tile) throws IOException, RuntimeException{
        URL url = new URL(DMRServiceURL);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");

        OutputStream os = connection.getOutputStream();
        os.write(geometry.getBytes());
        os.flush();

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Chyba při komunikaci s REST službou " + url.toString() + " => Chybový kód: "
                    + connection.getResponseCode());
        }

        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(new InputStreamReader((InputStream) connection.getContent()));
        JsonArray array = null;

        System.out.println(root);

        if (root.getAsJsonObject().has("samples")) {
            array = root.getAsJsonObject().get("samples").getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {
                float x = array.get(i).getAsJsonObject().get("location").getAsJsonObject().get("x").getAsFloat();
                float y = array.get(i).getAsJsonObject().get("location").getAsJsonObject().get("y").getAsFloat();
                float z = array.get(i).getAsJsonObject().get("value").getAsFloat();

                int insertCol = (int) (x - tile.getX()) / tile.getStep();
                int insertRow = (int) (tile.getY() - y) / tile.getStep();
                tile.setDataDMRItem(z, insertRow, insertCol);

                if (z < tile.getzMin()) tile.setzMin(z);
                if (z > tile.getzMax()) tile.setzMax(z);
            }
        } else {
            throw new RuntimeException("Nezmapovaná oblast");
        }

        connection.disconnect();
    }

    /**
     * Method constructs geometry for downloading tile data.
     * @param x tile start x coordinate
     * @param y tile start y coordinate
     * @return tile with all needed data
     * @throws IOException
     * @throws RuntimeException
     */
    private Tile makeDMRTile(double x, double y) throws IOException, RuntimeException {
        String points = "{\"points\":[";
        int counter = 1;

        Tile tile = new Tile(x, y);
        tile.setSize(tileSize);

        for (int j = (int) y ; j >= y - tileSize; j -= 2) {
            for (int i = (int) x; i <= x + tileSize; i += 2) {
                points += "[" + i + "," + j + "],";
                if (counter == 1000) {
                    // make request
                    points = points.substring(0, points.length()-1); //remove ' , ' at the end
                    points += "]}";
                    points = URLEncoder.encode(points, "UTF-8");
                    String geometry = "geometry=" + points + "&geometryType=esriGeometryMultipoint&returnFirstValueOnly=true&f=json";

                    downloadTileData(geometry, tile);

                    points = "{\"points\":[";
                    counter = 0;
                }
                counter++;
            }
        }

        points = points.substring(0, points.length()-1); //remove ' , ' at the end
        points += "]}";
        points = URLEncoder.encode(points, "UTF-8");
        String geometry = "geometry=" + points + "&geometryType=esriGeometryMultipoint&returnFirstValueOnly=true&f=json";

        downloadTileData(geometry, tile);
        downloadTexture(tile, 512);

        return tile;
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public void clearTiles() {
        tiles = null;
    }

    /**
     * Creates JSON representation for all tiles that were created.
     * @return JSON string
     */
    public String getTilesJSONRepresentation() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(this.tiles);
    }
}