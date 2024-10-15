/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;


import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
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
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

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
		final StructAttributeValues attributeValues = new StructAttributeValues( numberOfAttributeMappings, rawJdbcValues );
		int jdbcIndex = 0;
		for ( int i = 0; i < size; i++ ) {
			jdbcIndex += injectAttributeValue(
					getEmbeddedPart( embeddableMappingType, i ),
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
		final MappingType mappedType = modelPart.getMappedType();
		final int jdbcValueCount;
		final Object rawJdbcValue = rawJdbcValues[jdbcIndex];
		if ( mappedType instanceof EmbeddableMappingType ) {
			final EmbeddableMappingType embeddableMappingType = (EmbeddableMappingType) mappedType;
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
				attributeValues.setAttributeValue( attributeIndex, rawJdbcValue );
			}
			else {
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final Object[] subJdbcValues = new Object[jdbcValueCount];
				System.arraycopy( rawJdbcValues, jdbcIndex, subJdbcValues, 0, subJdbcValues.length );
				final StructAttributeValues subValues = getAttributeValues( embeddableMappingType, subJdbcValues, options );
				attributeValues.setAttributeValue(
						attributeIndex,
						instantiate( embeddableMappingType, subValues, options.getSessionFactory() )
				);
			}
		}
		else {
			assert modelPart.getJdbcTypeCount() == 1;
			jdbcValueCount = 1;
			final JdbcMapping jdbcMapping = modelPart.getSingleJdbcMapping();
			final Object jdbcValue = jdbcMapping.getJdbcJavaType().wrap(
					rawJdbcValue,
					options
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
		final Object[] jdbcValues;
		if ( valueCount != values.length || orderMapping != null ) {
			jdbcValues = new Object[valueCount];
		}
		else {
			jdbcValues = values;
		}
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
					getEmbeddedPart( embeddableMappingType, i ),
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
			StructAttributeValues attributeValues,
			SessionFactoryImplementor sessionFactory) {
		final EmbeddableRepresentationStrategy representationStrategy = embeddableMappingType.getRepresentationStrategy();
		final EmbeddableInstantiator instantiator;
		if ( !embeddableMappingType.isPolymorphic() ) {
			instantiator = representationStrategy.getInstantiator();
		}
		else {
			// the discriminator here is the composite class because it gets converted to the domain type when extracted
			instantiator = representationStrategy.getInstantiatorForClass( ( (Class<?>) attributeValues.getDiscriminator() ).getName() );
		}
		return instantiator.instantiate( attributeValues, sessionFactory );
	}

	public static ValuedModelPart getEmbeddedPart(EmbeddableMappingType embeddableMappingType, int position) {
		return position == embeddableMappingType.getNumberOfAttributeMappings()
				? embeddableMappingType.getDiscriminatorMapping()
				: embeddableMappingType.getAttributeMapping( position );
	}

	private static int injectJdbcValue(
			ValuedModelPart attributeMapping,
			Object[] attributeValues,
			int attributeIndex,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final int jdbcValueCount;
		if ( attributeMapping instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
			if ( toOneAttributeMapping.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
				return 0;
			}
			final ForeignKeyDescriptor foreignKeyDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
			final ValuedModelPart keyPart = foreignKeyDescriptor.getKeyPart();
			final Object foreignKeyValue = foreignKeyDescriptor.getAssociationKeyFromSide(
					attributeValues[attributeIndex],
					ForeignKeyDescriptor.Nature.TARGET,
					options.getSession()
			);
			if ( keyPart instanceof BasicValuedMapping ) {
				jdbcValueCount = 1;
				jdbcValues[jdbcIndex] = foreignKeyValue;
			}
			else if ( keyPart instanceof EmbeddableValuedModelPart ) {
				final EmbeddableMappingType mappingType = ( (EmbeddableValuedModelPart) keyPart ).getEmbeddableTypeDescriptor();
				jdbcValueCount = injectJdbcValues(
						mappingType,
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
					(valueIndex, objects, wrapperOptions, value, jdbcValueMapping) -> {
						objects[valueIndex] = value;
					},
					options.getSession()
			);
		}
		else if ( attributeMapping instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart embeddableValuedModelPart = (EmbeddableValuedModelPart) attributeMapping;
			final EmbeddableMappingType embeddableMappingType = embeddableValuedModelPart.getMappedType();
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
				jdbcValues[jdbcIndex] = embeddableMappingType.getAggregateMapping()
						.getJdbcMapping()
						.getJdbcValueBinder()
						.getBindValue(
								attributeValues[attributeIndex],
								options
						);
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
			final JdbcMapping jdbcMapping = attributeMapping.getSingleJdbcMapping();
			final Object relationalValue = jdbcMapping.convertToRelationalValue( attributeValues[attributeIndex] );
			if ( relationalValue != null ) {
				// Regardless how LOBs are bound by default, through structs we must use the native types
				switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
					case SqlTypes.BLOB:
					case SqlTypes.MATERIALIZED_BLOB:
						//noinspection unchecked,rawtypes
						jdbcValues[jdbcIndex] = ( (JavaType) jdbcMapping.getJdbcJavaType() ).unwrap(
								relationalValue,
								Blob.class,
								options
						);
						break;
					case SqlTypes.CLOB:
					case SqlTypes.MATERIALIZED_CLOB:
						//noinspection unchecked,rawtypes
						jdbcValues[jdbcIndex] = ( (JavaType) jdbcMapping.getJdbcJavaType() ).unwrap(
								relationalValue,
								Clob.class,
								options
						);
						break;
					case SqlTypes.NCLOB:
					case SqlTypes.MATERIALIZED_NCLOB:
						//noinspection unchecked,rawtypes
						jdbcValues[jdbcIndex] = ( (JavaType) jdbcMapping.getJdbcJavaType() ).unwrap(
								relationalValue,
								NClob.class,
								options
						);
						break;
					default:
						//noinspection unchecked
						jdbcValues[jdbcIndex] = jdbcMapping.getJdbcValueBinder().getBindValue(
								relationalValue,
								options
						);
						break;
				}
			}
		}
		return jdbcValueCount;
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
		final int jdbcValueCount;
		if ( mappedType instanceof EmbeddableMappingType ) {
			final EmbeddableMappingType subMappingType = (EmbeddableMappingType) mappedType;
			if ( subMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
			}
			else {
				jdbcValueCount = subMappingType.getJdbcValueCount();
			}
		}
		else {
			jdbcValueCount = 1;
		}
		return jdbcValueCount;
	}
}
