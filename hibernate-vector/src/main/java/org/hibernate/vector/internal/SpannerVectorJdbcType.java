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

public class SpannerVectorJdbcType extends ArrayJdbcType {

	private final int sqlType;
	private final String baseType;

	public SpannerVectorJdbcType(JdbcType elementJdbcType, int sqlType, String baseType) {
		super( elementJdbcType );
		this.sqlType = sqlType;
		this.baseType = baseType;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return sqlType;
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
				"FLOAT64".equalsIgnoreCase(baseType) ? double[].class : float[].class );
	}

	@Override
	protected String getElementTypeName(JavaType<?> javaType, SharedSessionContractImplementor session) {
		return baseType;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return new SpannerJdbcLiteralFormatterVector<>(
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
		appender.append( writeExpression );
	}

	@Override
	public boolean isWriteExpressionTyped(Dialect dialect) {
		return false;
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping, @Nullable Size size) {
		if ( sourceMapping.getJdbcType().isStringLike() ) {
			return "CASE WHEN ?1 IS NULL THEN NULL ELSE ARRAY(SELECT CAST(x AS " + baseType + ") FROM UNNEST(JSON_VALUE_ARRAY(?1)) x) END";
		}
		return null;
	}

	@Override
	public @Nullable String castToPattern(JdbcMapping targetJdbcMapping, @Nullable Size size) {
		if ( targetJdbcMapping.getJdbcType().isStringLike() ) {
			return "CASE WHEN ?1 IS NULL THEN NULL ELSE FORMAT('%T', ?1) END";
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
					return wrapVector(
							javaTypeDescriptor,
							"FLOAT64".equalsIgnoreCase( baseType ) ? parseDoubleVector( string ) : parseFloatVector( string ),
							options
					);
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
			&& that instanceof SpannerVectorJdbcType vectorJdbcType
			&& sqlType == vectorJdbcType.sqlType
			&& baseType.equals( vectorJdbcType.baseType );
	}

	@Override
	public int hashCode() {
		return sqlType + 31 * baseType.hashCode() + 31 * super.hashCode();
	}
}
