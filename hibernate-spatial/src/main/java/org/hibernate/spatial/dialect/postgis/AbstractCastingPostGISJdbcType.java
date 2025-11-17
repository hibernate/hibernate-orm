/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.spatial.GeometryLiteralFormatter;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecoder;
import org.geolatte.geom.codec.WktEncoder;

/**
 * Type Descriptor for the Postgis Geometry type
 *
 * @author Karel Maesen, Geovise BVBA
 */
public abstract class AbstractCastingPostGISJdbcType implements JdbcType {

	private final Wkb.Dialect wkbDialect;

	AbstractCastingPostGISJdbcType(Wkb.Dialect dialect) {
		wkbDialect = dialect;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new PGGeometryLiteralFormatter<>( getConstructorFunction(), javaType );
	}

	@Override
	public abstract int getDefaultSqlTypeCode();

	protected abstract String getConstructorFunction();

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( getConstructorFunction() );
		appender.append( '(' );
		appender.append( writeExpression );
		appender.append( ')' );
	}

	@Override
	public boolean isWriteExpressionTyped(Dialect dialect) {
		return true;
	}

	public Geometry<?> toGeometry(String wkt) {
		if ( wkt == null ) {
			return null;
		}
		if ( wkt.startsWith( "00" ) || wkt.startsWith( "01" ) ) {
			//we have a WKB because this wkt starts with the bit-order byte

			ByteBuffer buffer = ByteBuffer.from( wkt );
			final WkbDecoder decoder = Wkb.newDecoder( wkbDialect );
			return decoder.decode( buffer );
		}
		else {
			return parseWkt( wkt );
		}
	}

	private static Geometry<?> parseWkt(String pgValue) {
		final WktDecoder decoder = Wkt.newDecoder( Wkt.Dialect.POSTGIS_EWKT_1 );
		return decoder.decode( pgValue );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<X>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setString( index, toWkt( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, toWkt( value, options ) );
			}

			private String toWkt(X value, WrapperOptions options) throws SQLException {
				final WktEncoder encoder = Wkt.newEncoder( Wkt.Dialect.POSTGIS_EWKT_1 );
				final Geometry<?> geometry = getJavaType().unwrap( value, Geometry.class, options );
				return encoder.encode( geometry );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<X>( javaType, this ) {


			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( toGeometry( rs.getString( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( toGeometry( statement.getString( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap( toGeometry( statement.getString( name ) ), options );
			}
		};
	}

	static class PGGeometryLiteralFormatter<T> extends GeometryLiteralFormatter<T> {

		private final String constructorFunction;

		public PGGeometryLiteralFormatter(String constructorFunction, JavaType<T> javaType) {
			super( javaType, Wkt.Dialect.POSTGIS_EWKT_1, "" );
			this.constructorFunction = constructorFunction;
		}

		@Override
		public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
			Geometry<?> geom = javaType.unwrap( value, Geometry.class, wrapperOptions );
			appender.append( constructorFunction );
			appender.appendSql( "('" );
			appender.appendSql( Wkt.toWkt( geom, wktDialect ) );
			appender.appendSql( "')" );
		}
	}
}
