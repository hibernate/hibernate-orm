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
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.spi.AggregateJdbcValueOrder;
import org.hibernate.type.descriptor.jdbc.spi.AggregateJdbcValues;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class StructJdbcType implements StructuredJdbcType {

	public static final AggregateJdbcType INSTANCE = new StructJdbcType();

	private final String typeName;
	private final int[] orderMapping;
	private final int[] inverseOrderMapping;
	private final AggregateJdbcValueOrder valueOrder;
	private final EmbeddableMappingType embeddableMappingType;
	private final ValueExtractor<Object[]> objectArrayExtractor;

	private StructJdbcType() {
		// The default instance is for reading only and will return an Object[]
		this( null, null, null );
	}

	public StructJdbcType(EmbeddableMappingType embeddableMappingType, String typeName, int[] orderMapping) {
		this.embeddableMappingType = embeddableMappingType;
		this.typeName = typeName;
		this.orderMapping = orderMapping == null ? null : orderMapping.clone();
		if ( orderMapping == null ) {
			this.inverseOrderMapping = null;
			this.valueOrder = AggregateJdbcValueOrder.identity();
		}
		else {
			this.valueOrder = AggregateJdbcValueOrder.physicalOrder( this.orderMapping );
			final int[] inverseOrderMapping = new int[this.orderMapping.length];
			for ( int i = 0; i < this.orderMapping.length; i++ ) {
				inverseOrderMapping[this.orderMapping[i]] = i;
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
		final Object[] jdbcValues = AggregateJdbcValues.fromDomainValue(
				embeddableMappingType,
				domainValue,
				valueOrder,
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
		return StructHelper.toLogicalJdbcValues(
				embeddableMappingType,
				inverseOrderMapping,
				( (Struct) rawJdbcValue ).getAttributes(),
				options,
				this::transformRawJdbcValue
		);
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
					return javaType.cast( extractJdbcValues( struct, options ) );
				}
				assert embeddableMappingType != null && embeddableMappingType.getJavaType() == getJavaType();
				return javaType.cast( StructHelper.toDomainValue(
						embeddableMappingType,
						orderMapping,
						jdbcValues,
						options,
						StructJdbcType.this::transformRawJdbcValue
				) );
			}
		};
	}

	int[] getOrderMapping() {
		return orderMapping;
	}

	protected Object transformRawJdbcValue(Object rawJdbcValue, WrapperOptions options) {
		return rawJdbcValue;
	}

}
