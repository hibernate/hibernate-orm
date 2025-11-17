/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.sqlserver;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.sqlserver.Decoders;
import org.geolatte.geom.codec.db.sqlserver.Encoders;

/**
 * Type descriptor for the SQL Server 2008 Geometry type.
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 8/23/11
 */
public abstract class AbstractSqlServerGISType implements JdbcType {

	@Override
	public int getJdbcTypeCode() {
		return Types.ARRAY;
	}

	@Override
	public abstract int getDefaultSqlTypeCode();

	@Override
	public abstract <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType);

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<X>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Geometry geometry = getJavaType().unwrap( value, Geometry.class, options );
				final byte[] bytes = Encoders.encode( geometry );
				st.setObject( index, bytes );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Geometry geometry = getJavaType().unwrap( value, Geometry.class, options );
				final byte[] bytes = Encoders.encode( geometry );
				st.setObject( name, bytes );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<X>( javaType, this ) {

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( toGeometry( rs.getObject( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap( toGeometry( statement.getObject( name ) ), options );
			}
		};
	}

	private Geometry toGeometry(Object obj) {
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
