package cz.tul.drone.utils;

/**
 * @author ikopetschke
 */
public class KrovakWGS84Convertor extends AbstractCoordinatesConvertor {

    public KrovakWGS84Convertor() {
        super(WKTSEnum.MY_STRING_GT_S_JKTS_WGS84, WKIDEnum.SR_KROVAK_WGS84_ID, WKIDEnum.SR_KROVAK_ID, WKIDEnum.SR_WGS84_ID);
    }

}
