/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.StructuredJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.AggregateJdbcValueOrder;
import org.hibernate.type.descriptor.jdbc.spi.AggregateJdbcValues;
import org.hibernate.type.spi.TypeConfiguration;

/// Provider-owned structured JDBC type backed by a driver-specific document
/// container.
///
/// The provider owns the document container while Hibernate's aggregate JDBC
/// value facade owns mapping decomposition, component conversion, ordering,
/// and mapped-domain instantiation.
///
/// @author Steve Ebersole
/// @since 8.0
public final class ExampleStructuredJdbcType implements StructuredJdbcType {
	public static final AggregateJdbcType INSTANCE = new ExampleStructuredJdbcType();

	private final EmbeddableMappingType mappingType;
	private final String typeName;
	private final AggregateJdbcValueOrder valueOrder;

	private ExampleStructuredJdbcType() {
		this( null, null, AggregateJdbcValueOrder.identity() );
	}

	private ExampleStructuredJdbcType(
			EmbeddableMappingType mappingType,
			String typeName,
			AggregateJdbcValueOrder valueOrder) {
		this.mappingType = mappingType;
		this.typeName = typeName;
		this.valueOrder = valueOrder;
	}

	static ExampleStructuredJdbcType mapped(
			EmbeddableMappingType mappingType,
			AggregateJdbcValueOrder valueOrder) {
		return new ExampleStructuredJdbcType( mappingType, "example_document", valueOrder );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new ExampleStructuredJdbcType(
				mappingType,
				sqlType,
				AggregateJdbcValueOrder.identity()
		);
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return mappingType;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.STRUCT;
	}

	@Override
	public String getStructTypeName() {
		return typeName;
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return mappingType == null
				? typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Object[].class )
				: mappingType.getMappedJavaType();
	}

	@Override
	// tag::aggregate-jdbc-values[]
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		return new ExampleDocument(
				AggregateJdbcValues.fromDomainValue( mappingType, domainValue, valueOrder, options )
		);
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		return AggregateJdbcValues.toLogicalJdbcValues(
				mappingType,
				( (ExampleDocument) rawJdbcValue ).physicalValues(),
				valueOrder,
				options
		);
	}
	// end::aggregate-jdbc-values[]

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement statement, X value, int index, WrapperOptions options)
					throws SQLException {
				statement.setObject( index, createJdbcValue( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement statement, X value, String name, WrapperOptions options)
					throws SQLException {
				statement.setObject( name, createJdbcValue( value, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet resultSet, int index, WrapperOptions options) throws SQLException {
				return extract( resultSet.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				return extract( statement.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return extract( statement.getObject( name ), options );
			}

			private X extract(Object rawJdbcValue, WrapperOptions options) throws SQLException {
				if ( rawJdbcValue == null ) {
					return null;
				}
				final Object[] physicalValues = ( (ExampleDocument) rawJdbcValue ).physicalValues();
				return javaType.getJavaTypeClass() == Object[].class
						? javaType.cast( AggregateJdbcValues.toLogicalJdbcValues(
								mappingType,
								physicalValues,
								valueOrder,
								options
						) )
						: javaType.cast( AggregateJdbcValues.toDomainValue(
								mappingType,
								physicalValues,
								valueOrder,
								options
						) );
			}
		};
	}

	record ExampleDocument(Object[] physicalValues) {
	}
}
