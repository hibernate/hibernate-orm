/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// @author Steve Ebersole
class AggregateJdbcValuesTest {
	private final WrapperOptions options = mock( WrapperOptions.class );

	@Test
	void decomposesIntoPhysicalOrderWithoutAliasingTheDomainValues() throws SQLException {
		final EmbeddableMappingType mappingType = mappingType();
		final Object[] domainValue = { "A", "B", "C" };

		final Object[] jdbcValues = AggregateJdbcValues.fromDomainValue(
				mappingType,
				domainValue,
				AggregateJdbcValueOrder.physicalOrder( 2, 0, 1 ),
				options
		);

		assertArrayEquals( new Object[] { "C", "A", "B" }, jdbcValues );
		assertNotSame( domainValue, jdbcValues );
	}

	@Test
	void normalizesPhysicalValuesIntoLogicalOrderWithoutMutatingTheInput() throws SQLException {
		final EmbeddableMappingType mappingType = mappingType();
		final Object[] physicalValues = { "C", "A", "B" };

		final Object[] logicalValues = AggregateJdbcValues.toLogicalJdbcValues(
				mappingType,
				physicalValues,
				AggregateJdbcValueOrder.physicalOrder( 2, 0, 1 ),
				options
		);

		assertArrayEquals( new Object[] { "A", "B", "C" }, logicalValues );
		assertArrayEquals( new Object[] { "C", "A", "B" }, physicalValues );
	}

	@Test
	void instantiatesTheDomainValueFromPhysicalValues() throws SQLException {
		final Object domainValue = AggregateJdbcValues.toDomainValue(
				mappingType(),
				new Object[] { "C", "A", "B" },
				AggregateJdbcValueOrder.physicalOrder( 2, 0, 1 ),
				options
		);

		assertEquals( List.of( "A", "B", "C" ), domainValue );
	}

	@Test
	void rejectsComponentCountMismatchesBeforeConversion() throws SQLException {
		assertThrows(
				IllegalArgumentException.class,
				() -> AggregateJdbcValues.toLogicalJdbcValues(
						mappingType(),
						new Object[] { "A", "B" },
						options
				)
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
