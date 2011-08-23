package org.hibernate.spatial.dialect.sqlserver;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.spatial.HBSpatialExtension;
import org.hibernate.spatial.dialect.sqlserver.convertors.Decoders;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/11
 */
public class SqlServer2008GeometryValueExtractor implements ValueExtractor<Geometry> {

	@Override
	public Geometry extract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		Object geomObj = rs.getObject( name );
		return toJTS( geomObj );
	}

	public MGeometryFactory getGeometryFactory() {
		return HBSpatialExtension.getDefaultGeomFactory();
	}

	public Geometry toJTS(Object obj) {
		byte[] raw = null;
		if ( obj == null ) {
			return null;
		}
		if ( ( obj instanceof byte[] ) ) {
			raw = (byte[]) obj;
		}
		else if ( obj instanceof Blob ) {
			raw = toByteArray( (Blob) obj );
		}
		else {
			throw new IllegalArgumentException( "Expected byte array." );
		}
		return Decoders.decode( raw );
	}

	private byte[] toByteArray(Blob blob) {
		try {
			return blob.getBytes( 1, (int) blob.length() );
		}
		catch ( SQLException e ) {
			throw new RuntimeException( "Error on transforming blob into array.", e );
		}
	}

}
