/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class StructJdbcType implements org.hibernate.type.descriptor.jdbc.StructJdbcType {

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
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		if ( embeddableMappingType == null ) {
			return typeConfiguration.getJavaTypeRegistry().getDescriptor( Object[].class );
		}
		else {
			//noinspection unchecked
			return (JavaType<T>) embeddableMappingType.getMappedJavaType();
		}
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
				embeddableMappingType.getValues( domainValue ),
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
		final Object[] attributes = ( (Struct) rawJdbcValue ).getAttributes();
		wrapRawJdbcValues( embeddableMappingType, orderMapping, inverseOrderMapping, attributes, 0, options );
		return attributes;
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
				final Object[] values = struct.getAttributes();
				final boolean jdbcRepresentation = getJavaType().getJavaTypeClass() == Object[].class;
				if ( jdbcRepresentation ) {
					wrapRawJdbcValues( embeddableMappingType, orderMapping, inverseOrderMapping, values, 0, options );
					//noinspection unchecked
					return (X) values;
				}
				assert embeddableMappingType != null && embeddableMappingType.getJavaType() == getJavaType();
				final Object[] attributeValues = getAttributeValues(
						embeddableMappingType,
						orderMapping,
						values,
						options
				);
				//noinspection unchecked
				return (X) embeddableMappingType.getRepresentationStrategy().getInstantiator().instantiate(
						() -> attributeValues,
						options.getSessionFactory()
				);
			}
		};
	}

	private Object[] getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final Object[] attributeValues;
		if ( numberOfAttributeMappings != rawJdbcValues.length || orderMapping != null ) {
			attributeValues = new Object[numberOfAttributeMappings];
		}
		else {
			attributeValues = rawJdbcValues;
		}
		int jdbcIndex = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final int attributeIndex;
			if ( orderMapping == null ) {
				attributeIndex = i;
			}
			else {
				attributeIndex = orderMapping[i];
			}
			final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( attributeIndex );
			jdbcIndex += injectAttributeValue(
					attributeMapping,
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
				if ( rawJdbcValue == null ) {
					attributeValues[attributeIndex] = null;
				}
				else {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) embeddableMappingType.getAggregateMapping()
							.getJdbcMapping()
							.getJdbcType();
					final Object[] subValues;
					if ( aggregateJdbcType instanceof StructJdbcType ) {
						subValues = getAttributeValues(
								embeddableMappingType,
								( (StructJdbcType) aggregateJdbcType ).orderMapping,
								( (Struct) rawJdbcValue ).getAttributes(),
								options
						);
					}
					else {
						subValues = aggregateJdbcType.extractJdbcValues( rawJdbcValue, options );
					}
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
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final Object[] jdbcValues = new Object[jdbcValueCount];
				System.arraycopy( rawJdbcValues, jdbcIndex, jdbcValues, 0, jdbcValues.length );
				final Object[] subValues = getAttributeValues( embeddableMappingType, null, jdbcValues, options );
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
						final JdbcType elementJdbcType = elementType.getJdbcType();
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
								final EmbeddableInstantiator instantiator = subEmbeddableMappingType.getRepresentationStrategy()
										.getInstantiator();
								for ( int j = 0; j < array.length; j++ ) {
									final Object[] subValues = StructHelper.getAttributeValues(
											subEmbeddableMappingType,
											aggregateJdbcType.extractJdbcValues(
													array[j],
													options
											),
											options
									);
									newArray[j] = instantiator.instantiate(
											() -> subValues,
											options.getSessionFactory()
									);
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
			attributeValues[attributeIndex] = jdbcMapping.convertToDomainValue( jdbcValue );
		}
		return jdbcValueCount;
	}

	private int wrapRawJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			int[] inverseOrderMapping,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final Object[] targetJdbcValues;
		if ( orderMapping == null ) {
			targetJdbcValues = jdbcValues;
		}
		else {
			targetJdbcValues = jdbcValues.clone();
		}
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping;
			if ( orderMapping == null ) {
				attributeMapping = embeddableMappingType.getAttributeMapping( i );
			}
			else {
				attributeMapping = embeddableMappingType.getAttributeMapping( orderMapping[i] );
			}
			final MappingType mappedType = attributeMapping.getMappedType();

			if ( mappedType instanceof EmbeddableMappingType ) {
				final EmbeddableMappingType embeddableType = (EmbeddableMappingType) mappedType;
				if ( embeddableType.getAggregateMapping() != null ) {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) embeddableType.getAggregateMapping()
							.getJdbcMapping()
							.getJdbcType();
					final Object rawJdbcValue = targetJdbcValues[jdbcIndex];
					targetJdbcValues[jdbcIndex] = aggregateJdbcType.extractJdbcValues( rawJdbcValue, options );
					jdbcIndex++;
				}
				else {
					jdbcIndex = wrapRawJdbcValues( embeddableType, null, null, targetJdbcValues, jdbcIndex, options );
				}
			}
			else {
				assert attributeMapping.getJdbcTypeCount() == 1;
				final Object rawJdbcValue = targetJdbcValues[jdbcIndex];
				if ( rawJdbcValue != null ) {
					final JdbcMapping jdbcMapping = attributeMapping.getSingleJdbcMapping();
					switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
						case SqlTypes.TIME_WITH_TIMEZONE:
						case SqlTypes.TIME_UTC:
						case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
						case SqlTypes.TIMESTAMP_UTC:
							// Only transform the raw jdbc value if it could be a TIMESTAMPTZ
							targetJdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType()
									.wrap( transformRawJdbcValue( rawJdbcValue, options ), options );
							break;
						case SqlTypes.ARRAY:
							final BasicType<?> elementType = ( (BasicPluralType<?, ?>) jdbcMapping ).getElementType();
							final JdbcType elementJdbcType = elementType.getJdbcType();
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
									targetJdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( newArray, options );
									break;
								case SqlTypes.STRUCT:
								case SqlTypes.JSON:
								case SqlTypes.SQLXML:
									array = (Object[]) ( (java.sql.Array) rawJdbcValue ).getArray();
									newArray = new Object[array.length];
									final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) elementJdbcType;
									final EmbeddableMappingType subEmbeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
									final EmbeddableInstantiator instantiator = subEmbeddableMappingType.getRepresentationStrategy()
											.getInstantiator();
									for ( int j = 0; j < array.length; j++ ) {
										final Object[] subValues = StructHelper.getAttributeValues(
												subEmbeddableMappingType,
												aggregateJdbcType.extractJdbcValues(
														array[j],
														options
												),
												options
										);
										newArray[j] = instantiator.instantiate(
												() -> subValues,
												options.getSessionFactory()
										);
									}
									targetJdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( newArray, options );
									break;
								default:
									targetJdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
									break;
							}
							break;
						default:
							targetJdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
							break;
					}
				}
				jdbcIndex++;
			}
		}
		if ( orderMapping != null ) {
			StructHelper.orderJdbcValues( embeddableMappingType, inverseOrderMapping, targetJdbcValues, jdbcValues );
		}
		return jdbcIndex;
	}

	protected Object transformRawJdbcValue(Object rawJdbcValue, WrapperOptions options) {
		return rawJdbcValue;
	}

}
