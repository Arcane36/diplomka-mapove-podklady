package cz.tul.drone.utils;

/**
 * @author ikopetschke
 */
public class Test {

    /**
     * @param args the command line arguments
     */

    
    public static void main(String[] args) {
        KrovakWGS84Convertor c = new KrovakWGS84Convertor();
        System.out.println(c.convert(-686869.0, -973519.0 ));
        System.out.println(c.convertBack( 15.07271141131703, 50.7727518)); 
    }
    
}
