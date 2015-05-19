/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.sqlserver;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.geolatte.geom.Geometry;

import org.geolatte.geom.codec.db.sqlserver.Decoders;
import org.geolatte.geom.codec.db.sqlserver.Encoders;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Type descriptor for the SQL Server 2008 Geometry type.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/11
 */
public class SqlServer2008GeometryTypeDescriptor implements SqlTypeDescriptor {

	/**
	 * An instance of the descrtiptor
	 */
	public static final SqlServer2008GeometryTypeDescriptor INSTANCE = new SqlServer2008GeometryTypeDescriptor();

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
				final Geometry geometry = getJavaDescriptor().unwrap( value, Geometry.class, options );
				final byte[] bytes = Encoders.encode( geometry );
				st.setObject( index, bytes );
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

	private Geometry toGeometry(Object obj) {
		byte[] raw = null;
		if ( obj == null ) {
			return null;
		}
		if ( (obj instanceof byte[]) ) {
			raw = (byte[]) obj;
		}
		else if ( obj instanceof Blob ) {
			raw = toByteArray( (Blob) obj );
		}
		else {
			throw new IllegalArgumentException( "Expected byte array or BLOB" );
		}
		return Decoders.decode( raw );
	}

	private byte[] toByteArray(Blob blob) {
		try {
			return blob.getBytes( 1, (int) blob.length() );
		}
		catch (SQLException e) {
			throw new RuntimeException( "Error on transforming blob into array.", e );
		}
	}

}
