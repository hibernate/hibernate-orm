package org.hibernate.spatial.dialect.mysql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import org.hibernate.spatial.dialect.AbstractJTSGeometryValueExtractor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public class MySQLGeometryValueExtractor extends AbstractJTSGeometryValueExtractor {

    private static final int SRIDLEN = 4;

    /**
     * Converts the native geometry object to a JTS <code>Geometry</code>.
     *
     * @param object native database geometry object (depends on the JDBC spatial
     *               extension of the database)
     * @return JTS geometry corresponding to geomObj.
     */
    public Geometry toJTS(Object object) {
        if (object == null) {
            return null;
        }
        byte[] data = (byte[]) object;
        byte[] wkb = new byte[data.length - SRIDLEN];
        System.arraycopy(data, SRIDLEN, wkb, 0, wkb.length);
        int srid = 0;
        // WKB in MySQL Spatial is always little endian.
        srid = data[3] << 24 | (data[2] & 0xff) << 16 | (data[1] & 0xff) << 8
                | (data[0] & 0xff);
        Geometry geom = null;
        try {
            WKBReader reader = new WKBReader();
            geom = reader.read(wkb);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Couldn't parse incoming MySQL Spatial data.");
        }
        geom.setSRID(srid);
        return geom;
    }

}
