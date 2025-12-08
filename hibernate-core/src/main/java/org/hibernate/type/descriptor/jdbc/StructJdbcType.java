/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.descriptor.jdbc.StructHelper.getSubPart;
import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;

/**
 * @author Christian Beikov
 */
public class StructJdbcType implements StructuredJdbcType {

	public static final AggregateJdbcType INSTANCE = new StructJdbcType();

	private final String typeName;
	private final int[] orderMapping;
	private final int[] inverseOrderMapping;
	private final EmbeddableMappingType embeddableMappingType;
	private final ValueExtractor<Object[]> objectArrayExtractor;

	private StructJdbcType() {
		// The default instance is for reading only and will return an Object[]
		this( null, null, null );
	}

	public StructJdbcType(EmbeddableMappingType embeddableMappingType, String typeName, int[] orderMapping) {
		this.embeddableMappingType = embeddableMappingType;
		this.typeName = typeName;
		this.orderMapping = orderMapping;
		if ( orderMapping == null ) {
			this.inverseOrderMapping = null;
		}
		else {
			final int[] inverseOrderMapping = new int[orderMapping.length];
			for ( int i = 0; i < orderMapping.length; i++ ) {
				inverseOrderMapping[orderMapping[i]] = i;
			}
			this.inverseOrderMapping = inverseOrderMapping;
		}
		// We cache the extractor for Object[] here
		// since that is used in AggregateEmbeddableFetchImpl and AggregateEmbeddableResultImpl
		this.objectArrayExtractor = createBasicExtractor( new UnknownBasicJavaType<>( Object[].class ) );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.STRUCT;
	}

	@Override
	public String getStructTypeName() {
		return typeName;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new StructJdbcType(
				mappingType,
				sqlType,
				creationContext.getBootModel()
						.getDatabase()
						.getDefaultNamespace()
						.locateUserDefinedType( Identifier.toIdentifier( sqlType ) )
						.getOrderMapping()
		);
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return embeddableMappingType;
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return embeddableMappingType == null
				? typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Object[].class )
				: embeddableMappingType.getMappedJavaType();
	}

	@Override
	public void registerOutParameter(CallableStatement callableStatement, String name) throws SQLException {
		callableStatement.registerOutParameter( name, getJdbcTypeCode(), typeName );
	}

	@Override
	public void registerOutParameter(CallableStatement callableStatement, int index) throws SQLException {
		callableStatement.registerOutParameter( index, getJdbcTypeCode(), typeName );
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		final Object[] jdbcValues = StructHelper.getJdbcValues(
				embeddableMappingType,
				orderMapping,
				domainValue,
				options
		);
		return options.getSession()
				.getJdbcCoordinator()
				.getLogicalConnection()
				.getPhysicalConnection()
				.createStruct( typeName, jdbcValues );
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		final Object[] jdbcValues = ( (Struct) rawJdbcValue ).getAttributes();
		if ( orderMapping != null ) {
			StructHelper.orderJdbcValues( embeddableMappingType, inverseOrderMapping, jdbcValues.clone(), jdbcValues );
		}
		wrapRawJdbcValues( embeddableMappingType, jdbcValues, 0, options );
		return jdbcValues;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, createJdbcValue( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, createJdbcValue( value, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		if ( javaType.getJavaTypeClass() == Object[].class ) {
			//noinspection unchecked
			return (ValueExtractor<X>) objectArrayExtractor;
		}
		return createBasicExtractor( javaType );
	}

	private <X> BasicExtractor<X> createBasicExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getValue( rs.getObject( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getValue( statement.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getValue( statement.getObject( name ), options );
			}

			private X getValue(Object object, WrapperOptions options) throws SQLException {
				if ( object == null ) {
					return null;
				}
				final Struct struct = (Struct) object;
				final Object[] jdbcValues = struct.getAttributes();
				final boolean jdbcRepresentation = getJavaType().getJavaTypeClass() == Object[].class;
				if ( jdbcRepresentation ) {
					if ( orderMapping != null ) {
						StructHelper.orderJdbcValues( embeddableMappingType, inverseOrderMapping, jdbcValues.clone(), jdbcValues );
					}
					wrapRawJdbcValues( embeddableMappingType, jdbcValues, 0, options );
					return javaType.cast( jdbcValues );
				}
				assert embeddableMappingType != null && embeddableMappingType.getJavaType() == getJavaType();
				final StructAttributeValues attributeValues = getAttributeValues(
						embeddableMappingType,
						orderMapping,
						jdbcValues,
						options
				);
				return javaType.cast( instantiate( embeddableMappingType, attributeValues ) );
			}
		};
	}

	private StructAttributeValues getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final int size = numberOfAttributeMappings + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
		final StructAttributeValues attributeValues = new StructAttributeValues(
				numberOfAttributeMappings,
				orderMapping != null ?
						null :
						rawJdbcValues
		);
		int jdbcIndex = 0;
		for ( int i = 0; i < size; i++ ) {
			final int attributeIndex = orderMapping == null ? i : orderMapping[i];
			jdbcIndex += injectAttributeValue(
					getSubPart( embeddableMappingType, attributeIndex ),
					attributeValues,
					attributeIndex,
					rawJdbcValues,
					jdbcIndex,
					options
			);
		}
		return attributeValues;
	}

	private int injectAttributeValue(
			ValuedModelPart modelPart,
			StructAttributeValues attributeValues,
			int attributeIndex,
			Object[] rawJdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
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
								structJdbcType.orderMapping,
								( (Struct) rawJdbcValue ).getAttributes(),
								options
						);
					}
					else {
						subValues = StructHelper.getAttributeValues(
								embeddableMappingType,
								aggregateJdbcType.extractJdbcValues( rawJdbcValue, options ),
								options
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
				final StructAttributeValues subValues = getAttributeValues( embeddableMappingType, null, jdbcValues, options );
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
			final Object jdbcValue;
			if ( rawJdbcValue == null ) {
				jdbcValue = null;
			}
			else {
				switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
					case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
					case SqlTypes.TIMESTAMP_UTC:
						// Only transform the raw jdbc value if it could be a TIMESTAMPTZ
						jdbcValue = jdbcMapping.getJdbcJavaType()
								.wrap( transformRawJdbcValue( rawJdbcValue, options ), options );
						break;
					case SqlTypes.ARRAY:
						final BasicType<?> elementType = ( (BasicPluralType<?, ?>) jdbcMapping ).getElementType();
						final var elementJdbcType = elementType.getJdbcType();
						final Object[] array;
						final Object[] newArray;
						switch ( elementJdbcType.getDefaultSqlTypeCode() ) {
							case SqlTypes.TIME_WITH_TIMEZONE:
							case SqlTypes.TIME_UTC:
							case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
							case SqlTypes.TIMESTAMP_UTC:
								// Only transform the raw jdbc value if it could be a TIMESTAMPTZ
								array = (Object[]) ((java.sql.Array) rawJdbcValue).getArray();
								newArray = new Object[array.length];
								for ( int j = 0; j < array.length; j++ ) {
									newArray[j] = elementType.getJdbcJavaType().wrap(
											transformRawJdbcValue( array[j], options ),
											options
									);
								}
								jdbcValue = jdbcMapping.getJdbcJavaType().wrap( newArray, options );
								break;
							case SqlTypes.STRUCT:
							case SqlTypes.JSON:
							case SqlTypes.SQLXML:
								array = (Object[]) ( (java.sql.Array) rawJdbcValue ).getArray();
								newArray = new Object[array.length];
								final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) elementJdbcType;
								final EmbeddableMappingType subEmbeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
								for ( int j = 0; j < array.length; j++ ) {
									final StructAttributeValues subValues = StructHelper.getAttributeValues(
											subEmbeddableMappingType,
											aggregateJdbcType.extractJdbcValues(
													array[j],
													options
											),
											options
									);
									newArray[j] = instantiate( subEmbeddableMappingType, subValues );
								}
								jdbcValue = jdbcMapping.getJdbcJavaType().wrap( newArray, options );
								break;
							default:
								jdbcValue = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
								break;
						}
						break;
					default:
						jdbcValue = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
						break;
				}
			}
			attributeValues.setAttributeValue( attributeIndex, jdbcMapping.convertToDomainValue( jdbcValue ) );
		}
		return jdbcValueCount;
	}

	private int wrapRawJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < numberOfAttributeMappings + ( embeddableMappingType.isPolymorphic() ? 1 : 0 ); i++ ) {
			final ValuedModelPart attributeMapping = getSubPart( embeddableMappingType, i );
			if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
				if ( toOneAttributeMapping.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
					continue;
				}
				final ValuedModelPart keyPart = toOneAttributeMapping.getForeignKeyDescriptor().getKeyPart();
				if ( keyPart instanceof BasicValuedMapping ) {
					wrapRawJdbcValue( keyPart.getSingleJdbcMapping(), jdbcValues, jdbcIndex, options );
					jdbcIndex++;
				}
				else if ( keyPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
					final EmbeddableMappingType mappingType = embeddableValuedModelPart.getEmbeddableTypeDescriptor();
					jdbcIndex = wrapRawJdbcValues( mappingType, jdbcValues, jdbcIndex, options );
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
						discriminatedMapping.getDiscriminatorPart().getSingleJdbcMapping(),
						jdbcValues,
						jdbcIndex,
						options
				);
				jdbcIndex++;
				wrapRawJdbcValue(
						discriminatedMapping.getKeyPart().getSingleJdbcMapping(),
						jdbcValues,
						jdbcIndex,
						options
				);
				jdbcIndex++;
			}
			else if ( attributeMapping instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
				final EmbeddableMappingType embeddableType = embeddableValuedModelPart.getMappedType();
				if ( embeddableType.getAggregateMapping() != null ) {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType)
							embeddableType.getAggregateMapping().getJdbcMapping().getJdbcType();
					final Object rawJdbcValue = jdbcValues[jdbcIndex];
					jdbcValues[jdbcIndex] = aggregateJdbcType.extractJdbcValues( rawJdbcValue, options );
					jdbcIndex++;
				}
				else {
					jdbcIndex = wrapRawJdbcValues( embeddableType, jdbcValues, jdbcIndex, options );
				}
			}
			else {
				assert attributeMapping.getJdbcTypeCount() == 1;
				wrapRawJdbcValue( attributeMapping.getSingleJdbcMapping(), jdbcValues, jdbcIndex, options );
				jdbcIndex++;
			}
		}
		return jdbcIndex;
	}

	private void wrapRawJdbcValue(
			JdbcMapping jdbcMapping,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final Object rawJdbcValue = jdbcValues[jdbcIndex];
		if ( rawJdbcValue == null ) {
			return;
		}
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				// Only transform the raw jdbc value if it could be a TIMESTAMPTZ
				jdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType()
						.wrap( transformRawJdbcValue( rawJdbcValue, options ), options );
				break;
			case SqlTypes.ARRAY:
				final BasicType<?> elementType = ( (BasicPluralType<?, ?>) jdbcMapping ).getElementType();
				final var elementJdbcType = elementType.getJdbcType();
				final Object[] array;
				final Object[] newArray;
				switch ( elementJdbcType.getDefaultSqlTypeCode() ) {
					case SqlTypes.TIME_WITH_TIMEZONE:
					case SqlTypes.TIME_UTC:
					case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
					case SqlTypes.TIMESTAMP_UTC:
						// Only transform the raw jdbc value if it could be a TIMESTAMPTZ
						array = (Object[]) ((java.sql.Array) rawJdbcValue ).getArray();
						newArray = new Object[array.length];
						for ( int j = 0; j < array.length; j++ ) {
							newArray[j] = elementType.getJdbcJavaType().wrap(
									transformRawJdbcValue( array[j], options ),
									options
							);
						}
						jdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( newArray, options );
						break;
					case SqlTypes.STRUCT:
					case SqlTypes.JSON:
					case SqlTypes.SQLXML:
						array = (Object[]) ( (java.sql.Array) rawJdbcValue ).getArray();
						newArray = new Object[array.length];
						final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) elementJdbcType;
						final EmbeddableMappingType subEmbeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
						for ( int j = 0; j < array.length; j++ ) {
							final StructAttributeValues subValues = StructHelper.getAttributeValues(
									subEmbeddableMappingType,
									aggregateJdbcType.extractJdbcValues(
											array[j],
											options
									),
									options
							);
							newArray[j] = instantiate( subEmbeddableMappingType, subValues );
						}
						jdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( newArray, options );
						break;
					default:
						jdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
						break;
				}
				break;
			default:
				jdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
				break;
		}
	}

	protected Object transformRawJdbcValue(Object rawJdbcValue, WrapperOptions options) {
		return rawJdbcValue;
	}

}
