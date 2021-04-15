/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecoder;
import org.postgresql.util.PGobject;

/**
 * Type Descriptor for the Postgis Geometry type
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class PGGeometryTypeDescriptor implements SqlTypeDescriptor {


	private final Wkb.Dialect wkbDialect;

	// Type descriptor instance using EWKB v1 (postgis versions < 2.2.2)
	public static final PGGeometryTypeDescriptor INSTANCE_WKB_1 = new PGGeometryTypeDescriptor( Wkb.Dialect.POSTGIS_EWKB_1 );
	// Type descriptor instance using EWKB v2 (postgis versions >= 2.2.2, see: https://trac.osgeo.org/postgis/ticket/3181)
	public static final PGGeometryTypeDescriptor INSTANCE_WKB_2 = new PGGeometryTypeDescriptor( Wkb.Dialect.POSTGIS_EWKB_2 );

	private PGGeometryTypeDescriptor(Wkb.Dialect dialect) {
		wkbDialect = dialect;
	}

	public Geometry<?> toGeometry(Object object) {
		if ( object == null ) {
			return null;
		}
		ByteBuffer buffer = null;
		if ( object instanceof PGobject ) {
			String pgValue = ( (PGobject) object ).getValue();

			if ( pgValue.startsWith( "00" ) || pgValue.startsWith( "01" ) ) {
				//we have a WKB because this pgValue starts with the bit-order byte
				buffer = ByteBuffer.from( pgValue );
				final WkbDecoder decoder = Wkb.newDecoder( wkbDialect );
				return decoder.decode( buffer );
			}
			else {
				return parseWkt( pgValue );
			}

		}
		throw new IllegalStateException( "Received object of type " + object.getClass().getCanonicalName() );
	}

	private static Geometry<?> parseWkt(String pgValue) {
		final WktDecoder decoder = Wkt.newDecoder( Wkt.Dialect.POSTGIS_EWKT_1 );
		return decoder.decode( pgValue );
	}

	@Override
	public int getSqlType() {
		return 5432;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new ValueBinder<X>() {

			@Override
			public final void bind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				if ( value == null ) {
					st.setNull( index, Types.OTHER );
				}
				else {
					doBind( st, value, index, options );
				}
			}

			@Override
			public final void bind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				if ( value == null ) {
					st.setNull( name, Types.OTHER );
				}
				else {
					doBind( st, value, name, options );
				}
			}

			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final PGobject obj = toPGobject( value, options );
				st.setObject( index, obj );
			}

			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final PGobject obj = toPGobject( value, options );
				st.setObject( name, obj );
			}

			private PGobject toPGobject(X value, WrapperOptions options) throws SQLException {
				final WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.POSTGIS_EWKB_1 );
				final Geometry geometry = javaTypeDescriptor.unwrap( value, Geometry.class, options );
				final String hexString = encoder.encode( geometry, ByteOrder.NDR ).toString();
				final PGobject obj = new PGobject();
				obj.setType( "geometry" );
				obj.setValue( hexString );
				return obj;
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( rs.getObject( name ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getObject( name ) ), options );
			}
		};
	}
}
