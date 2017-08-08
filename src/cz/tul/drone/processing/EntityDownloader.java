package cz.tul.drone.processing;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import cz.tul.drone.utils.KrovakWGS84Convertor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Jan Kozánek on 15.03.2017.
 */
public class EntityDownloader {
    private ArrayList<Entity> entities = new ArrayList<>();
    private String OSMSavePath;
    private int tileSize = 128;
    private DBFactory db = null;
    private int entityPosition = 0;

    public ArrayList<Entity> getEntities() {
        return entities;
    }

    /**
     * Sets a save path for the OSM file and a db connection
     * @param OSMSavePath a file path for the OSM file
     */
    public EntityDownloader(DBFactory db, String OSMSavePath) {
        this.OSMSavePath = OSMSavePath;
        this.db = db;
    }

    /**
     * Starts the downloading and reading process for tiles around given coordinates
     * @param minX X coordinate in SJTSK
     * @param minY Y coordinate in SJTSK
     */
    public void downloadEntities(double minX, double minY, int numTilesAround) {
        downloadEntities(minX - (numTilesAround * tileSize), minY + (numTilesAround * tileSize), (2*numTilesAround + 1), (2*numTilesAround + 1));
    }

    /**
     * Starts the downloading and reading process in the uppermost left corner and continues for a given width and height
     * @param minX X coordinate in SJTSK
     * @param minY Y coordinate in SJTSK
     * @param width width for which to make tiles
     * @param height height for which to make tiles
     */
    public void downloadEntities(double minX, double minY, int width, int height) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                downloadEntities(minX + (tileSize*j), minY - (tileSize*i));
            }
        }
    }

    /**
     * Starts the downloading and reading process. If the entities are already in the database, it uses those instead of calculating them
     * @param minX X coordinate in SJTSK
     * @param minY Y coordinate in SJTSK
     */
    public void downloadEntities(double minX, double minY) {
        ResultSet entitiesFromDB = db.getEntitiesFromDB(minX, minY, this.tileSize);
        this.entityPosition = this.entities.size();

        try {
            if (!entitiesFromDB.next()) {
                getOSMData(minX, minY, this.OSMSavePath);
                processOSMFile(this.OSMSavePath);
                db.saveEntitiesToDB(this.entities, this.entityPosition, minX, minY, tileSize);
            } else {
                do {
                    Gson gson = new Gson();
                    Entity entity = new Entity();
                    entity.setId(entitiesFromDB.getString(1));
                    entity.setType(entitiesFromDB.getString(2));
                    entity.setMaxZ(entitiesFromDB.getFloat(3));
                    entity.setMinZ(entitiesFromDB.getFloat(4));
                    entity.setSJTSK(gson.fromJson(entitiesFromDB.getString(5), double[][].class));
                    this.entities.add(entity);
                } while(entitiesFromDB.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads an .osm file for a box specified by the top left corner of the box. The box default size is 128 x 128,
     * so the gps is converted to Krovak, the size is added to it to make a box and converted back
     * @param minLonKrovak x coordinate of the top left corner
     * @param minLatKrovak y coordiante of the top left corner
     * @param savePath path where the OSM files will be downloaded
     */
    private static void getOSMData(double minLonKrovak, double minLatKrovak, String savePath) {
        KrovakWGS84Convertor conv = new KrovakWGS84Convertor();

        //create the tile by defining min and max corners in Krovak
        Point minKrov = new Point(minLonKrovak, minLatKrovak);
        Point maxKrov = new Point();
        minKrov.setX(Math.floor(minKrov.getX()));
        minKrov.setY(Math.floor(minKrov.getY()));
        maxKrov.setX(Math.floor(minKrov.getX() + 128));
        maxKrov.setY(Math.floor(minKrov.getY() - 128));

        //encapsulate it in envelope
        Envelope ev = new Envelope();
        ev.setCoords(minKrov.getX(), minKrov.getY(), maxKrov.getX(), maxKrov.getY());

        Point evMin = conv.convert(ev.getXMin(), ev.getYMin());
        Point evMax = conv.convert(ev.getXMax(), ev.getYMax());
        ev.setCoords(evMin.getX(), evMin.getY(), evMax.getX(), evMax.getY());

        try {
            URL url =  new URL("http://api.openstreetmap.org/api/0.6/map?bbox="
                    + ev.getXMin() + "," + ev.getYMin() + "," + ev.getXMax() + "," + ev.getYMax());
            downloadFile(url, savePath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Download files from URL and saves it to desired location
     * @param url url of the file you want to download
     * @param savePath path where you want to save the file
     */
    private static void downloadFile(URL url, String savePath) {
        try (InputStream in = url.openStream()) {
            FileOutputStream fos = new FileOutputStream(new File(savePath));

            int length;
            byte[] buffer = new byte[1024];// buffer for portion of data from
            // connection
            while ((length = in.read(buffer)) > -1) {
                fos.write(buffer, 0, length);
            }

            fos.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the downloaded OSM file and processes it. First it gets a list of all entities of interest, then it
     * converts them to an object, finds coordinates to them and then calculates a max height for them
     * @param savedPath location fo the downloaded OSM file
     */
    private void processOSMFile(String savedPath) {
        ArrayList<Element> listOfXMLEntities;

        try {
            File inputFile = new File(savedPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            listOfXMLEntities = getNodesOfEntities(doc);
            makeEntityObjectsFromEntityXML(listOfXMLEntities);
            findCoordsFromEntities(doc, this.entities);
            findHeightsToEntities(this.entities);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method takes an OpenStreetMap XML and gets rid of everything that is not in an Entity with value that was specified in Enum file.
     * Only <way>...</way> tags with desired type will remain.
     * @param doc An XML OpenStreetMap doument containing information about a tile
     * @return An ArrayList of only those elements, that are in specified in the Enum
     */
    private static ArrayList<Element> getNodesOfEntities(Document doc) {
        ArrayList<Element> result = new ArrayList<>();
        String allowedEnum = AllowedEntityEnum.getAppendedString().toLowerCase();

        //find every element with "way" tag
        NodeList nList = doc.getElementsByTagName("way");

        //iterate through them
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;

                //get all "tag" tags of current node
                NodeList nListChildren = eElement.getElementsByTagName("tag");

                //iterate through them
                for (int child = 0; child < nListChildren.getLength(); child++) {
                    Node node1 = nListChildren.item(child);
                    if (node1.getNodeType() == Node.ELEMENT_NODE) {
                        Element tag = (Element) node1;
                        //check if it is desired obstacle
                        if ((tag.getAttribute("k")).matches(allowedEnum)) {
                            result.add(eElement);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * This method goes through every Entity in the given list, gets its id, type, references and encapsulates them in an Entity object.
     * In the end it adds given Entity in an ArrayList.
     * @param listOfXMLObstacles An ArrayList of Elements containing only those elements specified in the Enum
     * @return An ArrayList of Entities objects. These objects are still missing coordiantes. They only have id, type and references
     */
    private void makeEntityObjectsFromEntityXML(ArrayList<Element> listOfXMLObstacles) {
        String allowedEnum = AllowedEntityEnum.getAppendedString().toLowerCase();

        //iterate through all obstacles
        for (int i = 0; i < listOfXMLObstacles.size(); i++) {

            //get current node
            Node nNode = listOfXMLObstacles.get(i);
            Element eElement = (Element) nNode;

            //find nd nodes in current node (references) (sets Entity references)
            NodeList nListChildren = eElement.getElementsByTagName("nd");
            int num = nListChildren.getLength();
            Entity entity = new Entity(num);
            //iterate through all nd (reference) nodes
            for (int child = 0; child < nListChildren.getLength(); child++) {
                Node node1 = nListChildren.item(child);
                if (node1.getNodeType() == Node.ELEMENT_NODE) {
                    Element tag = (Element) node1;
                    //get ref attribute
                    entity.addReference(tag.getAttribute("ref"));
                }
            }

            //sets Entity id
            entity.setId(eElement.getAttribute("id"));

            //find tag nodes in current node (tag nodes have info like "building", "highway" etc.)
            NodeList tagListChildren = eElement.getElementsByTagName("tag");
            for (int child = 0; child < tagListChildren.getLength(); child++) {
                Node node1 = tagListChildren.item(child);
                if (node1.getNodeType() == Node.ELEMENT_NODE) {
                    Element tag = (Element) node1;
                    //check if it is desired entity (sets Entity type)
                    if ((tag.getAttribute("k")).matches(allowedEnum)) {
                        entity.setType(tag.getAttribute("k"));
                    }

                }
            }
            if (!entities.contains(entity)) {
                this.entities.add(entity);
            }
        }
    }

    /**
     * This method iterates through every reference in the Entity object and find an information about it in the OSM XML file.
     * Once it finds it, it gets its GPS location and then converts it to Krovak. The results are added to the Entity object and then to the array.
     * @param doc An Original OSM XML file with info about references
     * @param listOfObjectEntities An Entity object with references
     */
    private void findCoordsFromEntities(Document doc, ArrayList<Entity> listOfObjectEntities) {
        //find every element with "node" tag
        NodeList nList = doc.getElementsByTagName("node");

        //iterate through all obstacles
        for (Entity obs: listOfObjectEntities) {
            //iterate through the array of references in a given obstacle
            for (int i=0; i < obs.getReferences().size(); i++) {

                //find the reference in a list of nodes in osm file
                for (int j=0; j < nList.getLength(); j++) {
                    Node nNode =  nList.item(j);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;

                        //if the id of the object matches with the id in the OSM file, get its attributes
                        if (Objects.equals(eElement.getAttribute("id"), obs.getReferences().get(i))) {
                            double lat = Double.parseDouble(eElement.getAttribute("lat"));
                            double lon = Double.parseDouble(eElement.getAttribute("lon"));

                            double[] gpsPoint2D = new double[2];
                            gpsPoint2D[0] = lat;
                            gpsPoint2D[1] = lon;
                            obs.addWGS84OfReference(obs.getReferences().indexOf(eElement.getAttribute("id")), gpsPoint2D);

                            KrovakWGS84Convertor conv = new KrovakWGS84Convertor();
                            Point krovakPoint = conv.convertBack(lon, lat);
                            double[] krovakPoint2D = new double[2];
                            krovakPoint2D[0] = Math.floor(krovakPoint.getX());
                            krovakPoint2D[1] = Math.floor(krovakPoint.getY());

                            obs.addKrovakOfReference(obs.getReferences().indexOf(eElement.getAttribute("id")), krovakPoint2D);

                            String sURL = "http://ags.cuzk.cz/arcgis2/rest/services/dmp1g/ImageServer/getSamples" +
                                    "?f=json&geometry={x:" +krovakPoint.getX()+",y:"+krovakPoint.getY()+"}" +
                                    "&geometryType=esriGeometryPoint";

                            break;
                        }
                    }
                }
            }

            //if first coord is the same as the last one, add the coords to the end as well
            if (obs.getReferences().size() != 0 && obs.getReferences().get(0).equals(obs.getReferences().get(obs.getReferences().size()-1))) {
                obs.addWGS84OfReference(obs.getReferences().size() - 1, obs.getWGS84()[0]);
                obs.addKrovakOfReference(obs.getReferences().size() - 1, obs.getSJTSK()[0]);
            }
        }
    }

    /**
     * Iterates an arraylist of entities and sets height for each of them
     * @param listofObjectEntities an array of entities
     */
    private void findHeightsToEntities(ArrayList<Entity> listofObjectEntities) throws IOException {
        for (int i = 0; i < listofObjectEntities.size(); i++) {
            //continue if element is not highway (they don't have height)
            if (!Objects.equals(listofObjectEntities.get(i).getType(), AllowedEntityEnum.HIGHWAY.toString().toLowerCase())) {
                double[] heights = getHeightForEntity(listofObjectEntities.get(i));
                listofObjectEntities.get(i).setMaxZ(heights[1]);
                listofObjectEntities.get(i).setMinZ(heights[0]);
            }
        }
    }

    /**
     * Iterates through Krovak coordinates, appends them to a string and sends that string to ArcGIS REST API to get a height
     * @param entity entitiy for which I am finding a height
     * @return height of the entity
     */
    private static double[] getHeightForEntity(Entity entity) throws IOException {
        double[][] krovakArray = entity.getSJTSK();
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < krovakArray.length; j++) {
            sb.append("[" + krovakArray[j][0] + "," + krovakArray[j][1] + "],");
        }
        String geomURL = "geometryType=esriGeometryPolygon&geometry={\"rings\":[[" + sb.toString() + "]]}&f=json";
        return getHeightFromArcGIS(geomURL);
    }

    /**
     * Gets a json response from ArcGIS URL, parse it using Google's gson and gets a max height value from statistics
     * To avoid URL size limits, the method uses POST as a request method, so the geometry is send in body instead of in URL
     * Max height is from DMP server and Min height is from DMR server
     * @param geomUrl geometry data that is then POSTed in the body with the url (/dmp1g/ImageServer/computeStatisticsHistogram/...)
     * @return max and min heights for given entity in url
     * @throws IOException
     */
    private static double[] getHeightFromArcGIS(String geomUrl) throws IOException {
        double[] heights = new double[2];
        byte[] postData = geomUrl.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        String requestDMR = "http://ags.cuzk.cz/arcgis2/rest/services/dmr5g/ImageServer/computeStatisticsHistograms";

        URL url = new URL(requestDMR);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        conn.setRequestProperty("charset","utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
            JsonParser jp = new JsonParser();
            JsonElement root = jp.parse(new InputStreamReader((InputStream) conn.getContent()));
            heights[0] = root.getAsJsonObject().get("statistics").getAsJsonArray().get(0).getAsJsonObject().get("min").getAsDouble();
        } catch (NullPointerException e) {
            heights[0] = 999998;
        }


        String requestDMP = "http://ags.cuzk.cz/arcgis2/rest/services/dmp1g/ImageServer/computeStatisticsHistograms";

        url = new URL(requestDMP);
        conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        conn.setRequestProperty("charset","utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
            JsonParser jp = new JsonParser();
            JsonElement root = jp.parse(new InputStreamReader((InputStream) conn.getContent()));
            heights[1] = root.getAsJsonObject().get("statistics").getAsJsonArray().get(0).getAsJsonObject().get("max").getAsDouble();
        } catch (NullPointerException e) {
            heights[1] = 999999;
        }

        return heights;
    }

    /**
     * This method takes an envelope and moves it (centers) according to the upper left point
     * @param envelope envelope you want to center
     * @param tileSize size of a tile
     * @return centered tile according to upper left point
     */
    private static Envelope centerEnvelopeTile(Envelope envelope, int tileSize) {
        Envelope centered = new Envelope();
        centered.setCoords(
                envelope.getXMin() - tileSize / 2,
                envelope.getYMin() - tileSize / 2,
                envelope.getXMax() - tileSize / 2,
                envelope.getYMax() - tileSize / 2
        );
        return centered;
    }

}
