package org.hibernate.spatial.dialect.sqlserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.spatial.HBSpatialExtension;
import org.hibernate.spatial.dialect.sqlserver.convertors.Encoders;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/11
 */
public class SqlServer2008GeometryValueBinder implements ValueBinder<Geometry> {


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

	public MGeometryFactory getGeometryFactory() {
		return HBSpatialExtension.getDefaultGeomFactory();
	}

	public Object toNative(Geometry geom, Connection connection) {
		if ( geom == null ) {
			throw new IllegalArgumentException( "Null geometry passed." );
		}
		return Encoders.encode( geom );
	}

}
