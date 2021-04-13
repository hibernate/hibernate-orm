/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.mariadb;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;

public class MariaDBGeometryTypeDescriptor implements JdbcTypeDescriptor {

	public static final MariaDBGeometryTypeDescriptor INSTANCE = new MariaDBGeometryTypeDescriptor();
	final WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.MYSQL_WKB );
	final WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.MYSQL_WKB );

	@Override
	public int getJdbcType() {
		return Types.ARRAY;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {

		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(
					PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final byte[] bytes = valueToByteArray( value, options );
				st.setBytes( index, bytes );
			}

			@Override
			protected void doBind(
					CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
				final byte[] bytes = valueToByteArray( value, options );
				st.setBytes( name, bytes );
			}

			private byte[] valueToByteArray(X value, WrapperOptions options) {
				final Geometry<?> geometry = getJavaTypeDescriptor().unwrap( value, Geometry.class, options );
				final ByteBuffer buffer = encoder.encode( geometry, ByteOrder.NDR );
				return buffer == null ? null : buffer.toByteArray();
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {


			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( toGeometry( rs.getBytes( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( toGeometry( statement.getBytes( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaTypeDescriptor().wrap( toGeometry( statement.getBytes( name ) ), options );
			}
		};
	}

	private Geometry<?> toGeometry(byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		final ByteBuffer buffer = ByteBuffer.from( bytes );
		return decoder.decode( buffer );
	}
}
