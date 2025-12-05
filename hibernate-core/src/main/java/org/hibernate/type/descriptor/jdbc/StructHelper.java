/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;


import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A Helper for serializing and deserializing struct, based on an {@link EmbeddableMappingType}.
 */
@Internal
public class StructHelper {
	public static StructAttributeValues getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final int size = numberOfAttributeMappings + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
		final var attributeValues = new StructAttributeValues( numberOfAttributeMappings, rawJdbcValues );
		int jdbcIndex = 0;
		for ( int i = 0; i < size; i++ ) {
			jdbcIndex += injectAttributeValue(
					getSubPart( embeddableMappingType, i ),
					attributeValues,
					i,
					rawJdbcValues,
					jdbcIndex,
					options
			);
		}
		return attributeValues;
	}

	private static int injectAttributeValue(
			ValuedModelPart modelPart,
			StructAttributeValues attributeValues,
			int attributeIndex,
			Object[] rawJdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		if ( modelPart.getMappedType() instanceof EmbeddableMappingType embeddableMappingType ) {
			return injectAttributeValueEmbeddable(
					attributeValues, attributeIndex,
					rawJdbcValues, jdbcIndex,
					options,
					embeddableMappingType
			);
		}
		else {
			assert modelPart.getJdbcTypeCount() == 1;
			return injectAttributeValueSimple(
					modelPart,
					attributeValues, attributeIndex,
					rawJdbcValues, jdbcIndex,
					options
			);
		}
	}

	private static int injectAttributeValueSimple(
			ValuedModelPart modelPart,
			StructAttributeValues attributeValues,
			int attributeIndex,
			Object[] rawJdbcValues,
			int jdbcIndex,
			WrapperOptions options) {
		final var jdbcMapping = modelPart.getSingleJdbcMapping();
		final Object jdbcValue = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValues[jdbcIndex], options );
		attributeValues.setAttributeValue( attributeIndex, jdbcMapping.convertToDomainValue( jdbcValue ) );
		return 1;
	}

	private static int injectAttributeValueEmbeddable(
			StructAttributeValues attributeValues,
			int attributeIndex,
			Object[] rawJdbcValues,
			int jdbcIndex,
			WrapperOptions options,
			EmbeddableMappingType embeddableMappingType)
					throws SQLException {
		if ( embeddableMappingType.getAggregateMapping() != null ) {
			attributeValues.setAttributeValue( attributeIndex, rawJdbcValues[jdbcIndex] );
			return 1;
		}
		else {
			final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
			final Object[] subJdbcValues = new Object[jdbcValueCount];
			System.arraycopy( rawJdbcValues, jdbcIndex, subJdbcValues, 0, subJdbcValues.length );
			final var subValues = getAttributeValues( embeddableMappingType, subJdbcValues, options );
			attributeValues.setAttributeValue( attributeIndex, instantiate( embeddableMappingType, subValues ) );
			return jdbcValueCount;
		}
	}

	public static Object[] getJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object domainValue,
			WrapperOptions options) throws SQLException {
		final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
		final int valueCount = jdbcValueCount + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
		final Object[] values = embeddableMappingType.getValues( domainValue );
		final Object[] jdbcValues =
				valueCount != values.length || orderMapping != null ? new Object[valueCount] : values;
		injectJdbcValues(
				embeddableMappingType,
				values,
				jdbcValues,
				0,
				options
		);
		if ( orderMapping != null ) {
			final Object[] originalJdbcValues = jdbcValues.clone();
			for ( int i = 0; i < orderMapping.length; i++ ) {
				jdbcValues[i] = originalJdbcValues[orderMapping[i]];
			}
		}
		return jdbcValues;
	}

	private static int injectJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			@Nullable Object domainValue,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		return injectJdbcValues(
				embeddableMappingType,
				domainValue == null ? null : embeddableMappingType.getValues( domainValue ),
				jdbcValues,
				jdbcIndex,
				options
		);
	}

	private static int injectJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			@Nullable Object[] values,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
		final int valueCount = jdbcValueCount + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
		if ( values == null ) {
			return valueCount;
		}
		int offset = 0;
		for ( int i = 0; i < values.length; i++ ) {
			offset += injectJdbcValue(
					getSubPart( embeddableMappingType, i ),
					values,
					i,
					jdbcValues,
					jdbcIndex + offset,
					options
			);
		}
		assert offset == valueCount;
		return offset;
	}

	public static Object instantiate(
			EmbeddableMappingType embeddableMappingType,
			StructAttributeValues attributeValues) {
		return embeddableInstantiator( embeddableMappingType, attributeValues ).instantiate( attributeValues );
	}

	private static EmbeddableInstantiator embeddableInstantiator(
			EmbeddableMappingType embeddableMappingType,
			StructAttributeValues attributeValues) {
		final EmbeddableRepresentationStrategy representationStrategy = embeddableMappingType.getRepresentationStrategy();
		if ( !embeddableMappingType.isPolymorphic() ) {
			return representationStrategy.getInstantiator();
		}
		else {
			// the discriminator here is the composite class because it gets converted to the domain type when extracted
			final var discriminatorClass = (Class<?>) attributeValues.getDiscriminator();
			return representationStrategy.getInstantiatorForClass( discriminatorClass.getName() );
		}
	}

	public static ValuedModelPart getSubPart(ManagedMappingType type, int position) {
		if ( position == type.getNumberOfAttributeMappings() ) {
			assert type instanceof EmbeddableMappingType : "Unexpected position for non-embeddable type: " + type;
			return ( (EmbeddableMappingType) type ).getDiscriminatorMapping();
		}
		return type.getAttributeMapping( position );
	}

	private static int injectJdbcValue(
			ValuedModelPart attributeMapping,
			Object[] attributeValues,
			int attributeIndex,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final int jdbcValueCount;
		if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			if ( toOneAttributeMapping.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
				return 0;
			}
			final var foreignKeyDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
			final var keyPart = foreignKeyDescriptor.getKeyPart();
			final Object foreignKeyValue = foreignKeyDescriptor.getAssociationKeyFromSide(
					attributeValues[attributeIndex],
					ForeignKeyDescriptor.Nature.TARGET,
					options.getSession()
			);
			if ( keyPart instanceof BasicValuedMapping ) {
				jdbcValueCount = 1;
				jdbcValues[jdbcIndex] = foreignKeyValue;
			}
			else if ( keyPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
				jdbcValueCount = injectJdbcValues(
						embeddableValuedModelPart.getEmbeddableTypeDescriptor(),
						foreignKeyValue,
						jdbcValues,
						jdbcIndex,
						options
				);
			}
			else {
				throw new UnsupportedOperationException( "Unsupported foreign key part: " + keyPart );
			}
		}
		else if ( attributeMapping instanceof PluralAttributeMapping ) {
			return 0;
		}
		else if ( attributeMapping instanceof DiscriminatedAssociationAttributeMapping ) {
			jdbcValueCount = attributeMapping.decompose(
					attributeValues[attributeIndex],
					jdbcIndex,
					jdbcValues,
					options,
					(valueIndex, objects, wrapperOptions, value, jdbcValueMapping)
							-> objects[valueIndex] = value,
					options.getSession()
			);
		}
		else if ( attributeMapping instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			final EmbeddableMappingType embeddableMappingType = embeddableValuedModelPart.getMappedType();
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
				//noinspection unchecked
				jdbcValues[jdbcIndex] =
						embeddableMappingType.getAggregateMapping().getJdbcMapping().getJdbcValueBinder()
								.getBindValue( attributeValues[attributeIndex], options );
			}
			else {
				jdbcValueCount = injectJdbcValues(
						embeddableMappingType,
						attributeValues[attributeIndex],
						jdbcValues,
						jdbcIndex,
						options
				);
			}
		}
		else {
			assert attributeMapping.getJdbcTypeCount() == 1;
			jdbcValueCount = 1;
			final var jdbcMapping = attributeMapping.getSingleJdbcMapping();
			final Object relationalValue = jdbcMapping.convertToRelationalValue( attributeValues[attributeIndex] );
			if ( relationalValue != null ) {
				final var javaType = jdbcMapping.getJdbcJavaType();
				injectCastJdbcValue( jdbcValues, jdbcIndex, options, jdbcMapping, javaType, relationalValue );
			}
		}
		return jdbcValueCount;
	}

	private static <T> void injectCastJdbcValue(
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options,
			JdbcMapping jdbcMapping,
			JavaType<T> javaType,
			Object relationalValue)
			throws SQLException {
		assert javaType.isInstance( relationalValue );
		//noinspection unchecked
		injectJdbcValue( jdbcValues, jdbcIndex, options, jdbcMapping, javaType, (T) relationalValue );
	}

	private static <T> void injectJdbcValue(
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options,
			JdbcMapping jdbcMapping,
			JavaType<T> javaType,
			T relationalValue)
			throws SQLException {
		// Regardless how LOBs are bound by default, through structs we must use the native types
		jdbcValues[jdbcIndex] = switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.BLOB, SqlTypes.MATERIALIZED_BLOB ->
				javaType.unwrap( relationalValue, Blob.class, options );
			case SqlTypes.CLOB, SqlTypes.MATERIALIZED_CLOB ->
				javaType.unwrap( relationalValue, Clob.class, options );
			case SqlTypes.NCLOB, SqlTypes.MATERIALIZED_NCLOB ->
				javaType.unwrap( relationalValue, NClob.class, options );
			default ->
				//noinspection unchecked
				jdbcValues[jdbcIndex] =
						jdbcMapping.getJdbcValueBinder().getBindValue( relationalValue, options );
		};
	}

	/**
	 * The <code>sourceJdbcValues</code> array is ordered according to the expected physical order,
	 * as given through the argument order of @Instantiator.
	 * The <code>targetJdbcValues</code> array should be ordered according to the Hibernate internal ordering,
	 * which is based on property name.
	 * This method copies from <code>sourceJdbcValues</code> to <code>targetJdbcValues</code> according to the ordering.
	 */
	public static void orderJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			int[] inverseMapping,
			Object[] sourceJdbcValues,
			Object[] targetJdbcValues) {
		for ( int i = 0; i < inverseMapping.length; i++ ) {
			targetJdbcValues[i] = sourceJdbcValues[inverseMapping[i]];
		}

//		final int numberOfAttributes = embeddableMappingType.getNumberOfAttributeMappings();
//		int targetJdbcOffset = 0;
//		for ( int i = 0; i < numberOfAttributes + ( embeddableMappingType.isPolymorphic() ? 1 : 0 ); i++ ) {
//			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
//			final MappingType mappedType = attributeMapping.getMappedType();
//			final int jdbcValueCount = getJdbcValueCount( mappedType );
//
//			final int attributeIndex = inverseMapping[i];
//			int sourceJdbcIndex = 0;
//			for ( int j = 0; j < attributeIndex; j++ ) {
//				sourceJdbcIndex += getJdbcValueCount( embeddableMappingType.getAttributeMapping( j ).getMappedType() );
//			}
//
//			for ( int j = 0; j < jdbcValueCount; j++ ) {
//				targetJdbcValues[targetJdbcOffset++] = sourceJdbcValues[sourceJdbcIndex + j];
//			}
//		}
	}

	public static int getJdbcValueCount(MappingType mappedType) {
		return mappedType instanceof EmbeddableMappingType subMappingType
				? subMappingType.getAggregateMapping() != null ? 1
				: subMappingType.getJdbcValueCount() : 1;
	}
}
