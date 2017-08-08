package cz.tul.drone.processing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Created by Jan Kozánek on 15.03.2017.
 */
@XmlRootElement
public class Scene {

    @XmlElement
    private Tile[] tiles;
    @XmlElement
    private ArrayList<Entity> entities;

    public void makeScene(Tile[] tile, ArrayList<Entity> entities) {
        this.tiles = tile;
        this.entities = entities;
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public ArrayList<Entity> getEntities() {
        return entities;
    }

    public String getJson() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(this);
    }

    public String getPrettyJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(this);
    }

    public String getXML() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Scene.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(this, sw);
        return sw.toString();
    }
}
