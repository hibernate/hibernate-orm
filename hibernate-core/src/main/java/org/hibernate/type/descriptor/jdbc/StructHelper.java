/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

import jakarta.annotation.Nullable;
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
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A Helper for serializing and deserializing struct, based on an {@link EmbeddableMappingType}.
 */
@Internal
public class StructHelper {
	private static final RawJdbcValueTransformer IDENTITY_TRANSFORMER = (rawJdbcValue, options) -> rawJdbcValue;

	private StructHelper() {
	}

	@FunctionalInterface
	interface RawJdbcValueTransformer {
		Object transform(Object rawJdbcValue, WrapperOptions options);
	}

	public static StructAttributeValues getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		return getAttributeValues( embeddableMappingType, null, rawJdbcValues, options, IDENTITY_TRANSFORMER );
	}

	static StructAttributeValues getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final int size = numberOfAttributeMappings + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
		// Ordering describes JDBC slots, not attributes: a flattened embeddable
		// contributes several slots to a single attribute.
		final Object[] logicalJdbcValues;
		if ( orderMapping == null ) {
			logicalJdbcValues = rawJdbcValues;
		}
		else {
			logicalJdbcValues = new Object[rawJdbcValues.length];
			for ( int physicalIndex = 0; physicalIndex < orderMapping.length; physicalIndex++ ) {
				logicalJdbcValues[orderMapping[physicalIndex]] = rawJdbcValues[physicalIndex];
			}
		}
		final var attributeValues = new StructAttributeValues(
				numberOfAttributeMappings,
				null
		);
		int jdbcIndex = 0;
		for ( int i = 0; i < size; i++ ) {
			jdbcIndex += injectAttributeValue(
					getSubPart( embeddableMappingType, i ),
					attributeValues,
					i,
					logicalJdbcValues,
					jdbcIndex,
					options,
					rawJdbcValueTransformer
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
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		final MappingType mappedType = modelPart.getMappedType();
		final int jdbcValueCount;
		final Object rawJdbcValue = rawJdbcValues[jdbcIndex];
		if ( mappedType instanceof EmbeddableMappingType embeddableMappingType ) {
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
				if ( rawJdbcValue == null ) {
					attributeValues.setAttributeValue( attributeIndex, null );
				}
				else {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType)
							embeddableMappingType.getAggregateMapping().getJdbcMapping().getJdbcType();
					final StructAttributeValues subValues;
					if ( aggregateJdbcType instanceof StructJdbcType structJdbcType ) {
						subValues = getAttributeValues(
								embeddableMappingType,
								structJdbcType.getOrderMapping(),
								( (java.sql.Struct) rawJdbcValue ).getAttributes(),
								options,
								structJdbcType::transformRawJdbcValue
						);
					}
					else {
						subValues = getAttributeValues(
								embeddableMappingType,
								null,
								aggregateJdbcType.extractJdbcValues( rawJdbcValue, options ),
								options,
								IDENTITY_TRANSFORMER
						);
					}
					attributeValues.setAttributeValue(
							attributeIndex,
							instantiate( embeddableMappingType, subValues )
					);
				}
			}
			else {
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final Object[] jdbcValues = new Object[jdbcValueCount];
				System.arraycopy( rawJdbcValues, jdbcIndex, jdbcValues, 0, jdbcValues.length );
				final StructAttributeValues subValues = getAttributeValues(
						embeddableMappingType,
						null,
						jdbcValues,
						options,
						rawJdbcValueTransformer
				);
				attributeValues.setAttributeValue(
						attributeIndex,
						instantiate( embeddableMappingType, subValues )
				);
			}
		}
		else {
			assert modelPart.getJdbcTypeCount() == 1;
			jdbcValueCount = 1;
			final JdbcMapping jdbcMapping = modelPart.getSingleJdbcMapping();
			final Object jdbcValue = wrapRawJdbcValue(
					jdbcMapping,
					rawJdbcValue,
					options,
					rawJdbcValueTransformer
			);
			attributeValues.setAttributeValue( attributeIndex, jdbcMapping.convertToDomainValue( jdbcValue ) );
		}
		return jdbcValueCount;
	}

	public static Object[] getJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object domainValue,
			WrapperOptions options) throws SQLException {
		final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
		final int valueCount = jdbcValueCount + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
		final Object[] values = embeddableMappingType.getValues( domainValue );
		final Object[] jdbcValues = new Object[valueCount];
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

	public static Object[] toLogicalJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			int[] inverseOrderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		return toLogicalJdbcValues(
				embeddableMappingType,
				inverseOrderMapping,
				rawJdbcValues,
				options,
				IDENTITY_TRANSFORMER
		);
	}

	static Object[] toLogicalJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			int[] inverseOrderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		final Object[] jdbcValues = rawJdbcValues.clone();
		if ( inverseOrderMapping != null ) {
			orderJdbcValues( embeddableMappingType, inverseOrderMapping, rawJdbcValues, jdbcValues );
		}
		wrapRawJdbcValues( embeddableMappingType, jdbcValues, 0, options, rawJdbcValueTransformer );
		return jdbcValues;
	}

	public static Object toDomainValue(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		return toDomainValue(
				embeddableMappingType,
				orderMapping,
				rawJdbcValues,
				options,
				IDENTITY_TRANSFORMER
		);
	}

	static Object toDomainValue(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		return instantiate(
				embeddableMappingType,
				getAttributeValues(
						embeddableMappingType,
						orderMapping,
						rawJdbcValues,
						options,
						rawJdbcValueTransformer
				)
		);
	}

	private static int wrapRawJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < numberOfAttributeMappings + ( embeddableMappingType.isPolymorphic() ? 1 : 0 ); i++ ) {
			final ValuedModelPart attributeMapping = getSubPart( embeddableMappingType, i );
			if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
				if ( toOneAttributeMapping.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
					continue;
				}
				final ValuedModelPart keyPart = toOneAttributeMapping.getForeignKeyDescriptor().getKeyPart();
				if ( keyPart instanceof BasicValuedMapping ) {
					wrapRawJdbcValue( keyPart.getSingleJdbcMapping(), jdbcValues, jdbcIndex, options, rawJdbcValueTransformer );
					jdbcIndex++;
				}
				else if ( keyPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
					final EmbeddableMappingType mappingType = embeddableValuedModelPart.getEmbeddableTypeDescriptor();
					jdbcIndex = wrapRawJdbcValues(
							mappingType,
							jdbcValues,
							jdbcIndex,
							options,
							rawJdbcValueTransformer
					);
				}
				else {
					throw new UnsupportedOperationException( "Unsupported foreign key part: " + keyPart );
				}
			}
			else if ( attributeMapping instanceof PluralAttributeMapping ) {
				continue;
			}
			else if ( attributeMapping instanceof DiscriminatedAssociationAttributeMapping discriminatedMapping ) {
				wrapRawJdbcValue(
						discriminatedMapping.getDiscriminatorMapping().getSingleJdbcMapping(),
						jdbcValues,
						jdbcIndex++,
						options,
						rawJdbcValueTransformer
				);
				wrapRawJdbcValue(
						discriminatedMapping.getKeyPart().getSingleJdbcMapping(),
						jdbcValues,
						jdbcIndex++,
						options,
						rawJdbcValueTransformer
				);
			}
			else if ( attributeMapping instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
				final EmbeddableMappingType embeddableType = embeddableValuedModelPart.getMappedType();
				if ( embeddableType.getAggregateMapping() != null ) {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType)
							embeddableType.getAggregateMapping().getJdbcMapping().getJdbcType();
					final Object rawJdbcValue = jdbcValues[jdbcIndex];
					jdbcValues[jdbcIndex] = rawJdbcValue == null
							? null
							: aggregateJdbcType.extractJdbcValues( rawJdbcValue, options );
					jdbcIndex++;
				}
				else {
					jdbcIndex = wrapRawJdbcValues(
							embeddableType,
							jdbcValues,
							jdbcIndex,
							options,
							rawJdbcValueTransformer
					);
				}
			}
			else {
				assert attributeMapping.getJdbcTypeCount() == 1;
				wrapRawJdbcValue(
						attributeMapping.getSingleJdbcMapping(),
						jdbcValues,
						jdbcIndex++,
						options,
						rawJdbcValueTransformer
				);
			}
		}
		return jdbcIndex;
	}

	private static void wrapRawJdbcValue(
			JdbcMapping jdbcMapping,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		jdbcValues[jdbcIndex] = wrapRawJdbcValue(
				jdbcMapping,
				jdbcValues[jdbcIndex],
				options,
				rawJdbcValueTransformer
		);
	}

	private static Object wrapRawJdbcValue(
			JdbcMapping jdbcMapping,
			Object rawJdbcValue,
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		if ( rawJdbcValue == null ) {
			return null;
		}
		return switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.TIME_WITH_TIMEZONE,
					SqlTypes.TIME_UTC,
					SqlTypes.TIMESTAMP_WITH_TIMEZONE,
					SqlTypes.TIMESTAMP_UTC -> jdbcMapping.getJdbcJavaType().wrap(
					rawJdbcValueTransformer.transform( rawJdbcValue, options ),
					options
			);
			case SqlTypes.ARRAY -> wrapRawJdbcArray(
					jdbcMapping,
					rawJdbcValue,
					options,
					rawJdbcValueTransformer
			);
			default -> jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
		};
	}

	private static Object wrapRawJdbcArray(
			JdbcMapping jdbcMapping,
			Object rawJdbcValue,
			WrapperOptions options,
			RawJdbcValueTransformer rawJdbcValueTransformer) throws SQLException {
		final BasicType<?> elementType = ( (BasicPluralType<?, ?>) jdbcMapping ).getElementType();
		final JdbcType elementJdbcType = elementType.getJdbcType();
		return switch ( elementJdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.TIME_WITH_TIMEZONE,
					SqlTypes.TIME_UTC,
					SqlTypes.TIMESTAMP_WITH_TIMEZONE,
					SqlTypes.TIMESTAMP_UTC -> {
				final Object[] rawArray = (Object[]) ( (java.sql.Array) rawJdbcValue ).getArray();
				final Object[] array = new Object[rawArray.length];
				for ( int i = 0; i < rawArray.length; i++ ) {
					array[i] = elementType.getJdbcJavaType().wrap(
							rawJdbcValueTransformer.transform( rawArray[i], options ),
							options
					);
				}
				yield jdbcMapping.getJdbcJavaType().wrap( array, options );
			}
			case SqlTypes.STRUCT, SqlTypes.JSON, SqlTypes.SQLXML -> {
				final Object[] rawArray = (Object[]) ( (java.sql.Array) rawJdbcValue ).getArray();
				final Object[] array = new Object[rawArray.length];
				final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) elementJdbcType;
				final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
				for ( int i = 0; i < rawArray.length; i++ ) {
					if ( rawArray[i] != null ) {
						array[i] = instantiate(
								subMappingType,
								getAttributeValues(
										subMappingType,
										aggregateJdbcType.extractJdbcValues( rawArray[i], options ),
										options
								)
						);
					}
				}
				yield jdbcMapping.getJdbcJavaType().wrap( array, options );
			}
			default -> jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
		};
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
		injectJdbcValue( jdbcValues, jdbcIndex, options, jdbcMapping, javaType, javaType.cast( relationalValue ) );
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
	}

}
