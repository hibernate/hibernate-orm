/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.AggregateJdbcValueOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies that an independently compiled structured JDBC type may use the
/// aggregate JDBC-value facade without linking to Hibernate internals.
///
/// @author Steve Ebersole
class ExampleStructuredJdbcTypeTest {
	private final WrapperOptions options = mock( WrapperOptions.class );

	@Test
	void convertsBetweenDomainLogicalAndProviderPhysicalRepresentations() throws SQLException {
		final EmbeddableMappingType mappingType = mappingType();
		final ExampleStructuredJdbcType jdbcType = ExampleStructuredJdbcType.mapped(
				mappingType,
				AggregateJdbcValueOrder.physicalOrder( 2, 0, 1 )
		);
		final Object[] domainValue = { "A", "B", "C" };

		final ExampleStructuredJdbcType.ExampleDocument document =
				(ExampleStructuredJdbcType.ExampleDocument) jdbcType.createJdbcValue( domainValue, options );
		assertArrayEquals( new Object[] { "C", "A", "B" }, document.physicalValues() );
		assertArrayEquals( domainValue, jdbcType.extractJdbcValues( document, options ) );

		final ResultSet resultSet = mock( ResultSet.class );
		when( resultSet.getObject( 1 ) ).thenReturn( document );
		@SuppressWarnings("unchecked")
		final JavaType<List<String>> resultJavaType = mock( JavaType.class );
		when( resultJavaType.getJavaTypeClass() ).thenReturn( (Class) List.class );
		when( resultJavaType.cast( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		assertEquals(
				List.of( "A", "B", "C" ),
				jdbcType.getExtractor( resultJavaType ).extract( resultSet, 1, options )
		);
	}

	@SuppressWarnings("unchecked")
	private EmbeddableMappingType mappingType() throws SQLException {
		final EmbeddableMappingType mappingType = mock( EmbeddableMappingType.class );
		when( mappingType.getJdbcValueCount() ).thenReturn( 3 );
		when( mappingType.getNumberOfAttributeMappings() ).thenReturn( 3 );
		when( mappingType.getValues( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		for ( int i = 0; i < 3; i++ ) {
			final AttributeMapping modelPart = mock( AttributeMapping.class );
			final JdbcMapping jdbcMapping = mock( JdbcMapping.class );
			final JavaType<Object> jdbcJavaType = mock( JavaType.class );
			final ValueBinder<Object> valueBinder = mock( ValueBinder.class );
			when( mappingType.getAttributeMapping( i ) ).thenReturn( modelPart );
			when( modelPart.getJdbcTypeCount() ).thenReturn( 1 );
			when( modelPart.getSingleJdbcMapping() ).thenReturn( jdbcMapping );
			when( jdbcMapping.convertToRelationalValue( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
			when( jdbcMapping.convertToDomainValue( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
			doReturn( jdbcJavaType ).when( jdbcMapping ).getJdbcJavaType();
			when( jdbcMapping.getJdbcType() ).thenReturn( IntegerJdbcType.INSTANCE );
			when( jdbcMapping.getJdbcValueBinder() ).thenReturn( valueBinder );
			when( jdbcJavaType.isInstance( any() ) ).thenReturn( true );
			when( jdbcJavaType.cast( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
			when( jdbcJavaType.wrap( any(), any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
			when( valueBinder.getBindValue( any(), any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		}

		final EmbeddableRepresentationStrategy representationStrategy = mock( EmbeddableRepresentationStrategy.class );
		final EmbeddableInstantiator instantiator = mock( EmbeddableInstantiator.class );
		when( mappingType.getRepresentationStrategy() ).thenReturn( representationStrategy );
		when( representationStrategy.getInstantiator() ).thenReturn( instantiator );
		when( instantiator.instantiate( any() ) )
				.thenAnswer( invocation -> List.of( invocation.getArgument( 0, org.hibernate.metamodel.spi.ValueAccess.class ).getValues() ) );
		return mappingType;
	}
}
