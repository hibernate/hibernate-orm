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

import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

/**
 * A Helper for serializing and deserializing struct, based on an {@link EmbeddableMappingType}.
 */
@Internal
public class StructHelper {

	public static Object[] getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final Object[] attributeValues;
		if ( numberOfAttributeMappings != rawJdbcValues.length ) {
			attributeValues = new Object[numberOfAttributeMappings];
		}
		else {
			attributeValues = rawJdbcValues;
		}
		int jdbcIndex = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( i );
			jdbcIndex += injectAttributeValue( attributeMapping, attributeValues, i, rawJdbcValues, jdbcIndex, options );
		}
		return attributeValues;
	}

	private static int injectAttributeValue(
			AttributeMapping attributeMapping,
			Object[] attributeValues,
			int attributeIndex,
			Object[] rawJdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final MappingType mappedType = attributeMapping.getMappedType();
		final int jdbcValueCount;
		final Object rawJdbcValue = rawJdbcValues[jdbcIndex];
		if ( mappedType instanceof EmbeddableMappingType ) {
			final EmbeddableMappingType embeddableMappingType = (EmbeddableMappingType) mappedType;
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
				attributeValues[attributeIndex] = rawJdbcValue;
			}
			else {
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final Object[] subJdbcValues = new Object[jdbcValueCount];
				System.arraycopy( rawJdbcValues, jdbcIndex, subJdbcValues, 0, subJdbcValues.length );
				final Object[] subValues = getAttributeValues( embeddableMappingType, subJdbcValues, options );
				attributeValues[attributeIndex] = embeddableMappingType.getRepresentationStrategy()
						.getInstantiator()
						.instantiate(
								() -> subValues,
								embeddableMappingType.findContainingEntityMapping()
										.getEntityPersister()
										.getFactory()
						);
			}
		}
		else {
			assert attributeMapping.getJdbcTypeCount() == 1;
			jdbcValueCount = 1;
			final JdbcMapping jdbcMapping = attributeMapping.getSingleJdbcMapping();
			final Object jdbcValue = jdbcMapping.getJdbcJavaType().wrap(
					rawJdbcValue,
					options
			);
			attributeValues[attributeIndex] = jdbcMapping.convertToDomainValue( jdbcValue );
		}
		return jdbcValueCount;
	}

	public static Object[] getJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] attributeValues,
			WrapperOptions options) throws SQLException {
		final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
		final Object[] jdbcValues;
		if ( jdbcValueCount != attributeValues.length || orderMapping != null ) {
			jdbcValues = new Object[jdbcValueCount];
		}
		else {
			jdbcValues = attributeValues;
		}
		int jdbcIndex = 0;
		for ( int i = 0; i < attributeValues.length; i++ ) {
			final int attributeIndex;
			if ( orderMapping == null ) {
				attributeIndex = i;
			}
			else {
				attributeIndex = orderMapping[i];
			}
			jdbcIndex += injectJdbcValue(
					embeddableMappingType.getAttributeMapping( attributeIndex ),
					attributeValues,
					attributeIndex,
					jdbcValues,
					jdbcIndex,
					options
			);
		}
		assert jdbcIndex == jdbcValueCount;
		return jdbcValues;
	}

	private static int injectJdbcValue(
			AttributeMapping attributeMapping,
			Object[] attributeValues,
			int attributeIndex,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final MappingType mappedType = attributeMapping.getMappedType();
		final int jdbcValueCount;
		if ( mappedType instanceof EmbeddableMappingType ) {
			final EmbeddableMappingType embeddableMappingType = (EmbeddableMappingType) mappedType;
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) embeddableMappingType.getAggregateMapping()
						.getJdbcMapping()
						.getJdbcType();
				jdbcValueCount = 1;
				jdbcValues[jdbcIndex] = aggregateJdbcType.createJdbcValue(
						attributeValues[attributeIndex],
						options
				);
			}
			else {
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
				final Object[] subValues = embeddableMappingType.getValues( attributeValues[attributeIndex] );
				int offset = 0;
				for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
					offset += injectJdbcValue(
							embeddableMappingType.getAttributeMapping( i ),
							subValues,
							i,
							jdbcValues,
							jdbcIndex + offset,
							options
					);
				}
				assert offset == jdbcValueCount;
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
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		int targetJdbcOffset = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();
			final int jdbcValueCount = getJdbcValueCount( mappedType );

			final int attributeIndex = inverseMapping[i];
			int sourceJdbcIndex = 0;
			for ( int j = 0; j < attributeIndex; j++ ) {
				sourceJdbcIndex += getJdbcValueCount( embeddableMappingType.getAttributeMapping( j ).getMappedType() );
			}

			for ( int j = 0; j < jdbcValueCount; j++ ) {
				targetJdbcValues[targetJdbcOffset++] = sourceJdbcValues[sourceJdbcIndex + j];
			}
		}
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
