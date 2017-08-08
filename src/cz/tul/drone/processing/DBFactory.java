package cz.tul.drone.processing;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Jan Kozánek on 18.04.2017.
 */
public class DBFactory {
    private InitialContext init = null;
    private DataSource ds = null;
    private Connection con = null;
    private final String datasource = "java:comp/env/jdbc/kozanekDrony";

    public DBFactory() {
        try {
            this.init = new InitialContext();
            this.ds = (DataSource) init.lookup(datasource);
            this.con = ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public Connection getCon() {
        return con;
    }

    /**
     * Checks if there exists a tile with the coordinates in it.
     * @param x An x coordinate
     * @param y An y coordinate
     * @return A tile for corresponding coordinates
     */
    public ResultSet getTileForCoords(double x, double y) {
        ResultSet result = null;
        try {
            PreparedStatement prepare = this.con.prepareStatement(
                    "SELECT X, Y FROM Tile\n" +
                            "WHERE (X <= ? AND X +128 > ?)\n" +
                            "AND (Y >= ? AND Y - 128 < ?)\n");
            prepare.setDouble(1, x);
            prepare.setDouble(2, x);
            prepare.setDouble(3, y);
            prepare.setDouble(4, y);
            result = prepare.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Saves a tile into a database
     * @param tile A tile that is to be saved
     */
    public void saveTileToDB(Tile tile) {
        try {
            PreparedStatement prepare = this.con.prepareStatement("INSERT INTO Tile (x, y, zMin, zMax, size, step, texture, dataDMR) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ");
            prepare.setDouble(1, tile.getX());
            prepare.setDouble(2, tile.getY());
            prepare.setDouble(3, tile.getzMin());
            prepare.setDouble(4, tile.getzMax());
            prepare.setInt(5, tile.getSize());
            prepare.setInt(6, tile.getStep());
            prepare.setString(7, tile.getTexture());
            prepare.setString(8, tile.getDataDMRAsJSON());
            prepare.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Selects a tile for the uppermost left coordinate
     * @param x An uppermost left x coordinate
     * @param y An uppermost left y coordinate
     * @return A tile for given coordinates
     */
    public ResultSet getTileFromDB(double x, double y) {
        ResultSet result = null;
        try {
            PreparedStatement prepare = this.con.prepareStatement("SELECT * FROM Tile WHERE X = ? AND Y = ?");
            prepare.setDouble(1, x);
            prepare.setDouble(2, y);
            result = prepare.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Saves a set of entities to an EntityTile table.
     * @param entities An Arraylist of entities belonging to one specific tile
     * @param position A position
     * @param minX An x coordinate to which the entities belong to
     * @param minY An y coordinate to which the entities belong to
     * @param tileSize A tile size of a coordinate to which the entities belong to
     */
    public void saveEntitiesToDB(ArrayList<Entity> entities, int position, double minX, double minY, int tileSize) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        PreparedStatement prepare = null;
        PreparedStatement prepare2 = null;
        int index = 1;
        int index2 = 1;

        if (entities.size() > 0) {

            for(int i = position ; i < entities.size(); i++ ) {
                sb.append("(?, ?, ?, ?, ?),");
                sb2.append("(?, ?, ?, ?),");
            }

            if (!Objects.equals(sb.toString(), "") && !Objects.equals(sb2.toString(), "")) {
                try {
                    prepare = this.con.prepareStatement("INSERT IGNORE INTO Entity (id, type, maxZ, minZ, sjtsk) VALUES " + sb.deleteCharAt(sb.length()-1).toString());
                    prepare2 = this.con.prepareStatement("INSERT IGNORE INTO EntityTile (id, x, y, size) VALUES " + sb2.deleteCharAt(sb2.length()-1).toString());

                    for (int i = position; i < entities.size(); i++) {
                        prepare.setString(index++, entities.get(i).getId());
                        prepare.setString(index++, entities.get(i).getType());
                        prepare.setFloat(index++, (float) entities.get(i).getMaxZ());
                        prepare.setFloat(index++, (float) entities.get(i).getMinZ());
                        prepare.setString(index++, entities.get(i).getSJTSKinJSON());

                        prepare2.setString(index2++, entities.get(i).getId());
                        prepare2.setDouble(index2++, minX);
                        prepare2.setDouble(index2++, minY);
                        prepare2.setInt(index2++, tileSize);
                    }

                    prepare.executeUpdate();
                    prepare2.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    /**
     * Selects all entities belonging to a one specific tile consisting of x and y coordinates and a tile size
     * @param x An x coordinate of a tile
     * @param y An y coordinate of a tile
     * @param tileSize A tile size of a tile
     * @return A set of entities belonging to a tile
     */
    public ResultSet getEntitiesFromDB(double x, double y, int tileSize) {
        ResultSet result = null;

        try {
            PreparedStatement prepare = this.con.prepareStatement("SELECT Entity.* FROM Entity JOIN EntityTile ON Entity.id=EntityTile.id WHERE X=? AND Y=? AND size=?");
            prepare.setDouble(1, x);
            prepare.setDouble(2, y);
            prepare.setInt(3,tileSize);
            result = prepare.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Selects a single tile from database. If there are none, the db is empty.
     * @return true/false depending on wheter the db is empty or not
     */
    public boolean isDBEmpty() {
        ResultSet result = null;

        try {
            PreparedStatement prepare = this.con.prepareStatement("SELECT * FROM Tile LIMIT 1");
            result = prepare.executeQuery();

            if (!result.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Selects a minimal and maximal value of x and y coordinates for a whole cached map
     * @param tileSize A tile size
     * @return a Set of min and max for x and y
     */
    public ResultSet getMinMaxFromDB(int tileSize) {
        ResultSet result = null;

        try {
            PreparedStatement prepare = this.con.prepareStatement("SELECT MIN(X) AS min_X, MIN(Y) - ? AS min_Y, MAX(X) + ? AS max_X, MAX(Y) AS max_Y FROM Tile");
            prepare.setInt(1, tileSize);
            prepare.setInt(2, tileSize);
            result = prepare.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Positions the tile according to an envelope of Czech Republic
     * @param x An x coordiante in SJTSK
     * @param y An y coordinate in SJTSK
     * @param tileSize A tile size
     * @return Point positioned to a grid defined by an envelope of Czech Republic
     */
    public Point positionTileToGridAccordingToEnvelope(double x, double y, double tileSize) {
        Envelope bounds = new Envelope(-904704, -1227392, -431616, -935168);
        Point krovak = new Point();

        // Počet dlaždic oproti definovaný oblasti pro celou ČR
        double xTileNumber = Math.round((x - bounds.getXMin()) / tileSize);
        double yTileNumber = Math.round((bounds.getYMax() - y) / tileSize);

        // Souřadnice nový dlaždice umístěný do gridu
        krovak.setX(bounds.getXMin() + (xTileNumber * tileSize));
        krovak.setY(bounds.getYMax() - (yTileNumber * tileSize));
        return krovak;
    }

    /**
     * Aligns the given x and y coordinates according to a grid made by other already existing tiles in database
     * @param x An x coordinate
     * @param y An y coordinate
     * @param tileSize A tile size
     * @return A Point aligned in a grid
     */
    public Point positionTileToGrid(double x, double y, int tileSize) {
        ResultSet minmax = this.getMinMaxFromDB(tileSize);
        double tile_x = 0;
        double tile_y = 0;

        try {
            if (minmax.next()) {
                double min_x = minmax.getDouble(1);
                double min_y = minmax.getDouble(2);
                double max_x = minmax.getDouble(3);
                double max_y = minmax.getDouble(4);

                if (x < min_x) { //pokud je bod x pod nejmenší existující částí
                    while (min_x > x) {
                        min_x = min_x - tileSize;
                    }
                    tile_x = min_x;
                } else if (x > max_x) { //pokud je bod nad největší částí
                    while (max_x < x) {
                        max_x = max_x + tileSize;
                    }
                    tile_x = max_x - tileSize;
                } else { //pokud je bod v mezeře, hledám nejkratší cestu
                    //zprava blíž než zleva
                    if (x - min_x > max_x - x) {
                        while (max_x > x) {
                            max_x = max_x - tileSize;
                        }
                        tile_x = max_x;
                    } else {
                        while (min_x < x) {
                            min_x = min_x + tileSize;
                        }
                        tile_x = min_x - tileSize;
                    }
                }

                if (y < min_y) {
                    while (min_y > y) {
                        min_y = min_y - tileSize;
                    }
                    tile_y = min_y + tileSize;
                } else if (y > max_y) {
                    while (max_y < y) {
                        max_y = max_y + tileSize;
                    }
                    tile_y = max_y;
                } else {
                    if (y - min_y > max_y - y) {
                        while (max_y > y) {
                            max_y = max_y - tileSize;
                        }
                        tile_y = max_y + tileSize;
                    } else {
                        while (min_y < y) {
                            min_y = min_y + tileSize;
                        }
                        tile_y = min_y;
                    }
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        Point aligned = new Point(tile_x, tile_y);
        return aligned;

    }

}
