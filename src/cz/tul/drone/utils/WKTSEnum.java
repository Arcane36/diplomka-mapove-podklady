package cz.tul.drone.utils;

/**
 * @author ikopetschke
 */
public enum WKTSEnum {
    /**
     * Řetezec (WKTS) pro konverzi mezi kartografickým a geografickým zobrazením
     * Je přesnější než použití WKID
     * Odpovídá WKID 1623
     * @see http://resources.arcgis.com/en/help/arcgis-rest-api/02r3/02r3000000qq000000.htm
     * @see http://resources.arcgis.com/en/help/arcgis-rest-api/02r3/02r3000000r8000000.htm
     */
    MY_STRING_GT_S_JKTS_WGS84("\"GEOGTRAN[\"S_JTSK_To_WGS_1984_1\",GEOGCS[\"GCS_S_JTSK\",DATUM[\"D_S_JTSK\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],"
            + "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],"
            + "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],METHOD[\"Position_Vector\"],"
            + "PARAMETER[\"X_Axis_Translation\",570.83789],PARAMETER[\"Y_Axis_Translation\",85.682641],"
            + "PARAMETER[\"Z_Axis_Translation\",462.84673],PARAMETER[\"X_Axis_Rotation\",4.9984501],"
            + "PARAMETER[\"Y_Axis_Rotation\",1.5867074],PARAMETER[\"Z_Axis_Rotation\",5.2611106],PARAMETER[\"Scale_Difference\",3.5610256]]\"");
    
    private final String string;

    private WKTSEnum(String string) {
        this.string = string;
    }

    /**
     * Vraci konverzni retezec pro danou konverzi
     * @return WKTR
     */
    public String getString() {
        return string;
    }
    
    
    
}
