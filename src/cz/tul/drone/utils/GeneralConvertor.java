package cz.tul.drone.utils;

/**
 * @author ikopetschke
 */
public class GeneralConvertor extends AbstractCoordinatesConvertor{
    
    public GeneralConvertor(WKTSEnum wktsConversion, WKIDEnum wkidConversion, WKIDEnum wkidFrom, WKIDEnum wkidTo) {
        super(wktsConversion, wkidConversion, wkidFrom, wkidTo);
    }
}
