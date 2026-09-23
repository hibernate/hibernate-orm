/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hibernate.vector.internal.VectorHelper.parseDoubleVector;
import static org.hibernate.vector.internal.VectorHelper.parseFloatVector;

/**
 * JDBC type descriptor for Cloud Spanner PostgreSQL vector types (float4[] and float8[]).
 *
 * @since 7.2
 */
public class SpannerPostgreSQLVectorJdbcType extends ArrayJdbcType {

	private final int sqlType;
	private final String baseType;

	public SpannerPostgreSQLVectorJdbcType(int sqlTypeCode, String baseType, BasicType<?> elementBasicType) {
		this( elementBasicType.getJdbcType(), sqlTypeCode, baseType );
	}

	public SpannerPostgreSQLVectorJdbcType(JdbcType elementJdbcType, int sqlType, String baseType) {
		super( elementJdbcType );
		this.sqlType = sqlType;
		this.baseType = baseType;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return sqlType;
	}

	@Override
	protected String getElementTypeName(JavaType<?> javaType, SharedSessionContractImplementor session) {
		return baseType;
	}

	private boolean isDouble() {
		return "float8".equalsIgnoreCase( baseType )
				|| "FLOAT64".equalsIgnoreCase( baseType )
				|| "double precision".equalsIgnoreCase( baseType );
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
				isDouble() ? double[].class : float[].class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return new SpannerPostgreSQLJdbcLiteralFormatterVector<>(
				javaTypeDescriptor,
				getElementJdbcType().getJdbcLiteralFormatter( elementJavaType( javaTypeDescriptor ) ),
				baseType
		);
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		appender.appendSql( writeExpression );
	}

	@Override
	public boolean isWriteExpressionTyped(Dialect dialect) {
		return false;
	}

	@Override
	public @Nullable String castToPattern(JdbcMapping targetJdbcMapping, @Nullable Size size) {
		if ( targetJdbcMapping.getJdbcType().isStringLike() ) {
			return "cast(to_jsonb(?1) as text)";
		}
		return null;
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping, @Nullable Size size) {
		if ( sourceMapping.getJdbcType().isStringLike() ) {
			return ( isDouble() ? "spanner.float64_array(" : "spanner.float32_array(" )
					+ "cast(?1 as jsonb))";
		}
		return null;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return extract( rs.getObject( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return extract( statement.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return extract( statement.getObject( name ), options );
			}

			private X extract(Object value, WrapperOptions options) throws SQLException {
				if ( value == null ) {
					return null;
				}
				if ( value instanceof java.sql.Array array ) {
					try {
						final Object arrayData = array.getArray();
						return wrapVector( javaTypeDescriptor, arrayData, options );
					}
					finally {
						try {
							array.free();
						}
						catch (Exception ignored) {
						}
					}
				}
				if ( value instanceof String string ) {
					return wrapVector( javaTypeDescriptor, isDouble() ? parseDoubleVector( string ) : parseFloatVector( string ), options );
				}
				return wrapVector( javaTypeDescriptor, value, options );
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static <X> X wrapVector(JavaType<X> javaTypeDescriptor, Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		final Class<?> targetClass = javaTypeDescriptor.getJavaTypeClass();
		if ( targetClass == float[].class ) {
			if ( value instanceof float[] floats ) {
				return (X) floats;
			}
			if ( value instanceof Float[] boxed ) {
				final float[] result = new float[boxed.length];
				for ( int i = 0; i < boxed.length; i++ ) {
					result[i] = boxed[i] != null ? boxed[i] : 0.0f;
				}
				return (X) result;
			}
			if ( value instanceof double[] doubles ) {
				final float[] result = new float[doubles.length];
				for ( int i = 0; i < doubles.length; i++ ) {
					result[i] = (float) doubles[i];
				}
				return (X) result;
			}
			if ( value instanceof Double[] boxed ) {
				final float[] result = new float[boxed.length];
				for ( int i = 0; i < boxed.length; i++ ) {
					result[i] = boxed[i] != null ? boxed[i].floatValue() : 0.0f;
				}
				return (X) result;
			}
		}
		else if ( targetClass == double[].class ) {
			if ( value instanceof double[] doubles ) {
				return (X) doubles;
			}
			if ( value instanceof Double[] boxed ) {
				final double[] result = new double[boxed.length];
				for ( int i = 0; i < boxed.length; i++ ) {
					result[i] = boxed[i] != null ? boxed[i] : 0.0d;
				}
				return (X) result;
			}
			if ( value instanceof float[] floats ) {
				final double[] result = new double[floats.length];
				for ( int i = 0; i < floats.length; i++ ) {
					result[i] = (double) floats[i];
				}
				return (X) result;
			}
			if ( value instanceof Float[] boxed ) {
				final double[] result = new double[boxed.length];
				for ( int i = 0; i < boxed.length; i++ ) {
					result[i] = boxed[i] != null ? boxed[i].doubleValue() : 0.0d;
				}
				return (X) result;
			}
		}
		else if ( targetClass == Float[].class ) {
			if ( value instanceof Float[] boxed ) {
				return (X) boxed;
			}
			if ( value instanceof float[] floats ) {
				final Float[] result = new Float[floats.length];
				for ( int i = 0; i < floats.length; i++ ) {
					result[i] = floats[i];
				}
				return (X) result;
			}
			if ( value instanceof double[] doubles ) {
				final Float[] result = new Float[doubles.length];
				for ( int i = 0; i < doubles.length; i++ ) {
					result[i] = (float) doubles[i];
				}
				return (X) result;
			}
			if ( value instanceof Double[] boxed ) {
				final Float[] result = new Float[boxed.length];
				for ( int i = 0; i < boxed.length; i++ ) {
					result[i] = boxed[i] != null ? boxed[i].floatValue() : null;
				}
				return (X) result;
			}
		}
		else if ( targetClass == Double[].class ) {
			if ( value instanceof Double[] boxed ) {
				return (X) boxed;
			}
			if ( value instanceof double[] doubles ) {
				final Double[] result = new Double[doubles.length];
				for ( int i = 0; i < doubles.length; i++ ) {
					result[i] = doubles[i];
				}
				return (X) result;
			}
			if ( value instanceof float[] floats ) {
				final Double[] result = new Double[floats.length];
				for ( int i = 0; i < floats.length; i++ ) {
					result[i] = (double) floats[i];
				}
				return (X) result;
			}
			if ( value instanceof Float[] boxed ) {
				final Double[] result = new Double[boxed.length];
				for ( int i = 0; i < boxed.length; i++ ) {
					result[i] = boxed[i] != null ? boxed[i].doubleValue() : null;
				}
				return (X) result;
			}
		}
		return javaTypeDescriptor.wrap( value, options );
	}

	@Override
	public boolean equals(Object that) {
		return super.equals( that )
				&& that instanceof SpannerPostgreSQLVectorJdbcType vectorJdbcType
				&& sqlType == vectorJdbcType.sqlType
				&& baseType.equals( vectorJdbcType.baseType );
	}

	@Override
	public int hashCode() {
		return sqlType + 31 * baseType.hashCode() + 31 * super.hashCode();
	}

	@Override
	public String toString() {
		return "SpannerPostgreSQLVectorJdbcType(" + baseType + "[], " + sqlType + ")";
	}
}
