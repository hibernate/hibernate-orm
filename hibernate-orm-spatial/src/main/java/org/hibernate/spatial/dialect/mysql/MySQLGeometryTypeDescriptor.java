/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.mysql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Descriptor for MySQL Geometries.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class MySQLGeometryTypeDescriptor implements SqlTypeDescriptor {

	/**
	 * An instance of this Descriptor
	 */
	public static final MySQLGeometryTypeDescriptor INSTANCE = new MySQLGeometryTypeDescriptor();

	@Override
	public int getSqlType() {
		return Types.ARRAY;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.MYSQL_WKB );
				final Geometry geometry = getJavaDescriptor().unwrap( value, Geometry.class, options );
				final ByteBuffer buffer = encoder.encode( geometry, ByteOrder.NDR );
				final byte[] bytes = ( buffer == null ? null : buffer.toByteArray() );
				st.setBytes( index, bytes );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.MYSQL_WKB );
				final Geometry geometry = getJavaDescriptor().unwrap( value, Geometry.class, options );
				final ByteBuffer buffer = encoder.encode( geometry, ByteOrder.NDR );
				final byte[] bytes = ( buffer == null ? null : buffer.toByteArray() );
				st.setBytes( name, bytes );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( rs.getBytes( name ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getBytes( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getBytes( name ) ), options );
			}
		};
	}

	private Geometry toGeometry(byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		final ByteBuffer buffer = ByteBuffer.from( bytes );
		final WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.MYSQL_WKB );
		return decoder.decode( buffer );
	}

}
