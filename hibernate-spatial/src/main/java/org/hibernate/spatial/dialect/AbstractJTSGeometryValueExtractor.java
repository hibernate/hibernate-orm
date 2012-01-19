package org.hibernate.spatial.dialect;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.spatial.jts.JTS;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public abstract class AbstractJTSGeometryValueExtractor implements ValueExtractor<Geometry> {

    @Override
    public Geometry extract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
        Object geomObj = rs.getObject(name);
        return toJTS(geomObj);
    }

    protected MGeometryFactory getGeometryFactory() {
        return JTS.getDefaultGeomFactory();
    }

    //Note: access is public because it is also used in test class. Besides it is
    //side-effect free and doesn't use any intermediate state. So public access is safe.

    abstract public Geometry toJTS(Object object);

}
