/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.vector.SparseFloatVector;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PGSparseFloatVectorJdbcType extends ArrayJdbcType {

	public PGSparseFloatVectorJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.SPARSE_VECTOR_FLOAT32;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( float[].class );
	}

	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as sparsevec)" );
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping) {
		return sourceMapping.getJdbcType().isStringLike() ? "cast(?1 as sparsevec)" : null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setString( index, getString( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, getString( value, options ) );
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) {
				return getString( value, options );
			}

			private String getString(X value, WrapperOptions options) {
				final SparseFloatVector vector = getJavaType().unwrap( value, SparseFloatVector.class, options );
				final int size = vector.size();
				final int[] indices = vector.indices();
				final float[] floats = vector.floats();
				final StringBuilder sb = new StringBuilder( indices.length * 50 );
				char separator = '{';
				for ( int i = 0; i < indices.length; i++ ) {
					sb.append( separator );
					// The sparvec format is 1 based
					sb.append( indices[i] + 1 );
					sb.append( ':' );
					sb.append( floats[i] );
					separator = ',';
				}
				sb.append("}/");
				sb.append( size );
				return sb.toString();
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseSparseFloatVector( rs.getString( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseSparseFloatVector( statement.getString( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseSparseFloatVector( statement.getString( name ) ), options );
			}
		};
	}

	/**
	 * Parses the pgvector sparsevec format `{idx1:val1,idx2:val2}/size`.
	 */
	private static @Nullable SparseFloatVector parseSparseFloatVector(@Nullable String string) {
		if ( string == null ) {
			return null;
		}

		final int slashIndex = string.lastIndexOf( '/' );
		if ( string.charAt( 0 ) != '{' || slashIndex == -1 || string.charAt( slashIndex - 1 ) != '}' ) {
			throw new IllegalArgumentException( "Invalid sparse vector string: " + string );
		}
		final int size = Integer.parseInt( string, slashIndex + 1, string.length(), 10 );
		final int end = slashIndex - 1;
		final int count = countValues( string, end );
		final int[] indices = new int[count];
		final float[] values = new float[count];
		int start = 1;
		int index = 0;
		for ( int i = start; i < end; i++ ) {
			final char c = string.charAt( i );
			if ( c == ':' ) {
				// Indices are 1 based in this format, but we need a zero base
				indices[index] = Integer.parseInt( string, start, i, 10 ) - 1;
				start = i + 1;
			}
			else if ( c == ',' ) {
				values[index++] = Float.parseFloat( string.substring( start, i ) );
				start = i + 1;
			}
		}
		if ( start != end ) {
			values[index] = Float.parseFloat( string.substring( start, end ) );
			assert count == index + 1;
		}
		return new SparseFloatVector( size, indices, values );
	}

	private static int countValues(String string, int end) {
		int count = 0;
		for ( int i = 1; i < end; i++ ) {
			if ( string.charAt( i ) == ':' ) {
				count++;
			}
		}
		return count;
	}
}
