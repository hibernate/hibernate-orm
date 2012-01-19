package org.hibernate.spatial.dialect;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.spatial.jts.JTS;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public abstract class AbstractJTSGeometryValueBinder implements ValueBinder<Geometry> {

   	@Override
	public void bind(PreparedStatement st, Geometry value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.STRUCT );
		}
		else {
			Geometry jtsGeom = (Geometry) value;
			Object dbGeom = toNative( jtsGeom, st.getConnection() );
			st.setObject( index, dbGeom );
		}
	}

    protected MGeometryFactory getGeometryFactory() {
		return JTS.getDefaultGeomFactory();
	}

    protected abstract Object toNative(Geometry jtsGeom, Connection connection);
}
