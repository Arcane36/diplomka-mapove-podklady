package cz.tul.drone.utils;

/**
 * @author ikopetschke
 */
public enum WKIDEnum {
    /**
     * WKID prostorové reference (spatial reference) kartografického zobrazení (projected coordinate systems) v systému S-JKTS (Křovák)
     * Hodnota odpovídá S-JTSK_Krovak_East_North, použitelné například pro ČR
     * Používané pro konverzi mezi dalšími systémy
     * @see http://resources.arcgis.com/en/help/arcgis-rest-api/02r3/02r3000000vt000000.htm
     */
    SR_KROVAK_ID(5514),
    
    /**
     * WKID prostorové reference (spatial reference) geografického zobrazení (geographic coordinate systems) v systému WGS_1984 (GPS)
     * Hodnota odpovídá GCS_WGS_1984, použitelné například pro ČR
     * Používané pro konverzi mezi dalšími systémy
     * @see http://resources.arcgis.com/en/help/arcgis-rest-api/02r3/02r300000105000000.htm
     */
    SR_WGS84_ID(4326),
    
    /**
     * WKID konverzní reference (spatial reference) mezi kartografickým a geografickým zobrazením
     * Hodnota odpovídá S-JTSK_Krovak_East_North, použitelné například pro ČR
     * Používané pro konverzi mezi dalšími systémy
     * @see http://resources.arcgis.com/en/help/arcgis-rest-api/02r3/02r300000105000000.htm
     */
    SR_KROVAK_WGS84_ID(1623);
    
    private final int id;

    private WKIDEnum(int id) {
        this.id = id;
    }
    
    /**
     * Vraci WKID pro spatial reference
     * @return WKID
     */
    public int getId() {
        return id;
    }
    
    
    
}
