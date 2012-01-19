package org.hibernate.spatial.dialect.mysql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ByteOrderValues;
import com.vividsolutions.jts.io.WKBWriter;
import org.hibernate.spatial.dialect.AbstractJTSGeometryValueBinder;
import org.hibernate.spatial.jts.JTS;

import java.sql.Connection;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public class MySQLGeometryValueBinder extends AbstractJTSGeometryValueBinder {

    private static final int SRIDLEN = 4;

    @Override
    protected Object toNative(Geometry jtsGeom, Connection connection) {
        if (jtsGeom.isEmpty()) return null;
        jtsGeom = forceGeometryCollection(jtsGeom);
        int srid = jtsGeom.getSRID();

        WKBWriter writer = new WKBWriter(2,
                ByteOrderValues.LITTLE_ENDIAN);
        byte[] wkb = writer.write(jtsGeom);

        byte[] byteArr = new byte[wkb.length + SRIDLEN];
        byteArr[3] = (byte) ((srid >> 24) & 0xFF);
        byteArr[2] = (byte) ((srid >> 16) & 0xFF);
        byteArr[1] = (byte) ((srid >> 8) & 0xFF);
        byteArr[0] = (byte) (srid & 0xFF);
        System.arraycopy(wkb, 0, byteArr, SRIDLEN, wkb.length);
        return byteArr;
    }

    private Geometry forceGeometryCollection(Geometry jtsGeom) {
        if (jtsGeom.isEmpty()) return createEmptyGeometryCollection(jtsGeom);
        if (jtsGeom instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) jtsGeom;
            Geometry[] components = new Geometry[gc.getNumGeometries()];
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                Geometry component = gc.getGeometryN(i);
                if (component.isEmpty()) {
                    components[i] = jtsGeom.getFactory().createGeometryCollection(null);
                } else {
                    components[i] = component;
                }
            }
            Geometry geometryCollection = jtsGeom.getFactory().createGeometryCollection(components);
            geometryCollection.setSRID(jtsGeom.getSRID());
            return geometryCollection;
        }
        return jtsGeom;
    }

    private Geometry createEmptyGeometryCollection(Geometry jtsGeom) {
        GeometryFactory factory = jtsGeom.getFactory();
        if (factory == null) {
            factory = JTS.getDefaultGeomFactory();
        }
        Geometry empty = factory.createGeometryCollection(null);
        empty.setSRID(jtsGeom.getSRID());
        return empty;
    }
}
