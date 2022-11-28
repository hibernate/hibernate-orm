/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class OracleStructJdbcType implements AggregateJdbcType {

	public static final AggregateJdbcType INSTANCE = new OracleStructJdbcType();

	private static final ClassValue<Method> RAW_JDBC_TRANSFORMER = new ClassValue<>() {
		@Override
		protected Method computeValue(Class<?> type) {
			if ( "oracle.sql.TIMESTAMPTZ".equals( type.getName() ) ) {
				try {
					return type.getMethod( "offsetDateTimeValue", Connection.class );
				}
				catch (NoSuchMethodException e) {
					throw new RuntimeException( e );
				}
			}
			return null;
		}
	};

	private final String oracleTypeName;
	private final EmbeddableMappingType embeddableMappingType;
	private final ValueExtractor<Object[]> objectArrayExtractor;

	private OracleStructJdbcType() {
		// The default instance is for reading only and will return an Object[]
		this( null, null );
	}

	public OracleStructJdbcType(EmbeddableMappingType embeddableMappingType, String typeName) {
		this.oracleTypeName = typeName == null ? null : typeName.toUpperCase( Locale.ROOT );
		this.embeddableMappingType = embeddableMappingType;
		// We cache the extractor for Object[] here
		// since that is used in AggregateEmbeddableFetchImpl and AggregateEmbeddableResultImpl
		this.objectArrayExtractor = createBasicExtractor( new UnknownBasicJavaType<>( Object[].class ) );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.STRUCT;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(EmbeddableMappingType mappingType, String sqlType) {
		return new OracleStructJdbcType( mappingType, sqlType );
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
		callableStatement.registerOutParameter( name, getJdbcTypeCode(), oracleTypeName );
	}

	@Override
	public void registerOutParameter(CallableStatement callableStatement, int index) throws SQLException {
		callableStatement.registerOutParameter( index, getJdbcTypeCode(), oracleTypeName );
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		final Object[] jdbcValues = StructHelper.getJdbcValues(
				embeddableMappingType,
				embeddableMappingType.getValues( domainValue ),
				options
		);
		return options.getSession()
				.getJdbcCoordinator()
				.getLogicalConnection()
				.getPhysicalConnection()
				.createStruct( oracleTypeName, jdbcValues );
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		final Object[] attributes = ( (Struct) rawJdbcValue ).getAttributes();
		wrapRawJdbcValues( embeddableMappingType, attributes, 0, options );
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
					wrapRawJdbcValues( embeddableMappingType, values, 0, options );
					//noinspection unchecked
					return (X) values;
				}
				assert embeddableMappingType != null && embeddableMappingType.getJavaType() == getJavaType();
				final Object[] attributeValues = getAttributeValues(
						embeddableMappingType,
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
				if ( rawJdbcValue instanceof Struct ) {
					final Object[] subValues = getAttributeValues(
							embeddableMappingType,
							( (Struct) rawJdbcValue ).getAttributes(),
							options
					);
					attributeValues[attributeIndex] = embeddableMappingType.getRepresentationStrategy()
							.getInstantiator()
							.instantiate(
									() -> subValues,
									embeddableMappingType.findContainingEntityMapping()
											.getEntityPersister()
											.getFactory()
							);
				}
				else {
					attributeValues[attributeIndex] = rawJdbcValue;
				}
			}
			else {
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final Object[] jdbcValues = new Object[jdbcValueCount];
				System.arraycopy( rawJdbcValues, jdbcIndex, jdbcValues, 0, jdbcValues.length );
				final Object[] subValues = getAttributeValues( embeddableMappingType, jdbcValues, options );
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
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMappings().get( 0 );
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
					default:
						jdbcValue = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
						break;
				}
			}
			attributeValues[attributeIndex] = jdbcMapping.convertToDomainValue( jdbcValue );
		}
		return jdbcValueCount;
	}

	private static int wrapRawJdbcValues(
			EmbeddableMappingType embeddableMappingType,
			Object[] jdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();

			if ( mappedType instanceof EmbeddableMappingType ) {
				final EmbeddableMappingType embeddableType = (EmbeddableMappingType) mappedType;
				if ( embeddableType.getAggregateMapping() != null ) {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) embeddableType.getAggregateMapping()
							.getJdbcMapping()
							.getJdbcType();
					jdbcValues[jdbcIndex] = aggregateJdbcType.extractJdbcValues( jdbcValues[jdbcIndex], options );
					jdbcIndex++;
				}
				else {
					jdbcIndex = wrapRawJdbcValues( embeddableType, jdbcValues, jdbcIndex, options );
				}
			}
			else {
				assert attributeMapping.getJdbcTypeCount() == 1;
				final Object rawJdbcValue = jdbcValues[jdbcIndex];
				if ( rawJdbcValue != null ) {
					final JdbcMapping jdbcMapping = attributeMapping.getJdbcMappings().get( 0 );
					switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
						case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
						case SqlTypes.TIMESTAMP_UTC:
							// Only transform the raw jdbc value if it could be a TIMESTAMPTZ
							jdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType()
									.wrap( transformRawJdbcValue( rawJdbcValue, options ), options );
							break;
						default:
							jdbcValues[jdbcIndex] = jdbcMapping.getJdbcJavaType().wrap( rawJdbcValue, options );
							break;
					}
				}
				jdbcIndex++;
			}
		}
		return jdbcIndex;
	}

	private static Object transformRawJdbcValue(Object rawJdbcValue, WrapperOptions options) {
		Method rawJdbcTransformer = RAW_JDBC_TRANSFORMER.get( rawJdbcValue.getClass() );
		if ( rawJdbcTransformer == null ) {
			return rawJdbcValue;
		}
		try {
			return rawJdbcTransformer.invoke(
					rawJdbcValue,
					options.getSession()
							.getJdbcCoordinator()
							.getLogicalConnection()
							.getPhysicalConnection()
			);
		}
		catch (Exception e) {
			throw new HibernateException( "Could not transform the raw jdbc value", e );
		}
	}

}
