/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicCollectionType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.vector.SparseFloatVector;

import java.util.Arrays;
import java.util.List;


public class SparseFloatVectorJavaType extends AbstractClassJavaType<SparseFloatVector> implements BasicPluralJavaType<Float> {

	public static final SparseFloatVectorJavaType INSTANCE = new SparseFloatVectorJavaType();

	public SparseFloatVectorJavaType() {
		super( SparseFloatVector.class, new SparseVectorMutabilityPlan() );
	}

	@Override
	public JavaType<Float> getElementJavaType() {
		return FloatJavaType.INSTANCE;
	}

	@Override
	public BasicType<?> resolveType(TypeConfiguration typeConfiguration, Dialect dialect, BasicType<Float> elementType, ColumnTypeInformation columnTypeInformation, JdbcTypeIndicators stdIndicators) {
		final int arrayTypeCode = stdIndicators.getPreferredSqlTypeCodeForArray( elementType.getJdbcType().getDefaultSqlTypeCode() );
		final JdbcType arrayJdbcType = typeConfiguration.getJdbcTypeRegistry()
				.resolveTypeConstructorDescriptor( arrayTypeCode, elementType, columnTypeInformation );
		if ( elementType.getValueConverter() != null ) {
			throw new IllegalArgumentException( "Can't convert element type of sparse vector" );
		}
		return typeConfiguration.getBasicTypeRegistry()
				.resolve( this, arrayJdbcType,
						() -> new BasicCollectionType<>( elementType, arrayJdbcType, this, "sparse_float_vector" ) );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( SqlTypes.SPARSE_VECTOR_INT8 );
	}

	@Override
	public <X> X unwrap(SparseFloatVector value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( type.isInstance( value ) ) {
			//noinspection unchecked
			return (X) value;
		}
		else if ( float[].class.isAssignableFrom( type ) ) {
			return (X) value.toDenseVector();
		}
		else if ( Object[].class.isAssignableFrom( type ) ) {
			//noinspection unchecked
			return (X) value.toArray();
		}
		else if ( String.class.isAssignableFrom( type ) ) {
			//noinspection unchecked
			return (X) value.toString();
		}
		else {
			throw unknownUnwrap( type );
		}
	}

	@Override
	public <X> SparseFloatVector wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if (value instanceof SparseFloatVector vector) {
			return vector;
		}
		else if (value instanceof List<?> list) {
			//noinspection unchecked
			return new SparseFloatVector( (List<Float>) list );
		}
		else if (value instanceof Object[] array) {
			//noinspection unchecked
			return new SparseFloatVector( (List<Float>) (List<?>) Arrays.asList( array ) );
		}
		else if (value instanceof float[] vector) {
			return new SparseFloatVector( vector );
		}
		else if (value instanceof String vector) {
			return new SparseFloatVector( vector );
		}
		else {
			throw unknownWrap( value.getClass() );
		}
	}

	private static class SparseVectorMutabilityPlan extends MutableMutabilityPlan<SparseFloatVector> {
		@Override
		protected SparseFloatVector deepCopyNotNull(SparseFloatVector value) {
			return value.clone();
		}
	}
}
