/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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
	void roundTripsFlattenedEmbeddableWithInterleavedPhysicalSlots() throws SQLException {
		final EmbeddableMappingType nested = mappingType( 2 );
		final EmbeddableMappingType mapping = mappingType( 2 );
		when( mapping.getJdbcValueCount() ).thenReturn( 3 );
		final AttributeMapping embedded = mock(
				AttributeMapping.class, withSettings().extraInterfaces( EmbeddableValuedModelPart.class )
		);
		when( embedded.getMappedType() ).thenReturn( nested );
		when( mapping.getAttributeMapping( 0 ) ).thenReturn( embedded );
		final Object[] domain = { new Object[] { "A", null }, "C" };
		final AggregateJdbcValueOrder order = AggregateJdbcValueOrder.physicalOrder( 2, 0, 1 );
		final Object[] physical = AggregateJdbcValues.fromDomainValue( mapping, domain, order, options );
		assertArrayEquals( new Object[] { "C", "A", null }, physical );
		assertArrayEquals( new Object[] { "A", null, "C" },
				AggregateJdbcValues.toLogicalJdbcValues( mapping, physical, order, options ) );
		assertEquals( Arrays.asList( Arrays.asList( "A", null ), "C" ),
				AggregateJdbcValues.toDomainValue( mapping, physical, order, options ) );
		assertArrayEquals( new Object[] { "C", "A", null }, physical );
	}

	@Test
	void roundTripsNestedNativeAggregateWithoutDecodingItTwice() throws SQLException {
		final EmbeddableMappingType nested = mappingType( 2 );
		final EmbeddableMappingType mapping = mappingType( 2 );
		final AttributeMapping embedded = mock(
				AttributeMapping.class, withSettings().extraInterfaces( EmbeddableValuedModelPart.class )
		);
		when( embedded.getMappedType() ).thenReturn( nested );
		when( mapping.getAttributeMapping( 0 ) ).thenReturn( embedded );
		final SelectableMapping selectable = mock( SelectableMapping.class );
		when( nested.getAggregateMapping() ).thenReturn( selectable );
		final JdbcMapping jdbcMapping = mock( JdbcMapping.class );
		when( selectable.getJdbcMapping() ).thenReturn( jdbcMapping );
		final AggregateJdbcType aggregate = mock( AggregateJdbcType.class );
		when( jdbcMapping.getJdbcType() ).thenReturn( aggregate );
		final ValueBinder<Object> binder = mock( ValueBinder.class );
		when( jdbcMapping.getJdbcValueBinder() ).thenReturn( binder );
		when( binder.getBindValue( any(), any() ) ).thenReturn( "native-container" );
		when( aggregate.extractJdbcValues( "native-container", options ) ).thenReturn( new Object[] { "A", null } );
		final AggregateJdbcValueOrder order = AggregateJdbcValueOrder.physicalOrder( 1, 0 );
		final Object[] physical = AggregateJdbcValues.fromDomainValue( mapping,
				new Object[] { new Object[] { "A", null }, "B" }, order, options );
		assertArrayEquals( new Object[] { "B", "native-container" }, physical );
		assertEquals( Arrays.asList( Arrays.asList( "A", null ), "B" ),
				AggregateJdbcValues.toDomainValue( mapping, physical, order, options ) );
		assertArrayEquals( new Object[] { "B", "native-container" }, physical );
		assertEquals( Arrays.asList( null, "B" ), AggregateJdbcValues.toDomainValue(
				mapping, new Object[] { "B", null }, order, options ) );
	}

	@Test
	@SuppressWarnings("unchecked")
	void convertsArrayComponentsWithoutMutatingTheDecodedContainer() throws SQLException {
		final EmbeddableMappingType mapping = mappingType( 2 );
		final BasicPluralType<Object[], Object> arrayType = mock( BasicPluralType.class );
		final BasicType<Object> elementType = mock( BasicType.class );
		when( arrayType.getElementType() ).thenReturn( elementType );
		when( elementType.getJdbcType() ).thenReturn( IntegerJdbcType.INSTANCE );
		when( arrayType.getJdbcType() ).thenReturn( new ArrayJdbcType( IntegerJdbcType.INSTANCE ) );
		final JavaType<Object[]> javaType = mock( JavaType.class );
		doReturn( javaType ).when( arrayType ).getJdbcJavaType();
		when( javaType.wrap( any(), any() ) ).thenAnswer( invocation ->
				( (java.sql.Array) invocation.getArgument( 0 ) ).getArray() );
		when( arrayType.convertToDomainValue( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		when( mapping.getAttributeMapping( 0 ).getSingleJdbcMapping() ).thenReturn( arrayType );
		final java.sql.Array nativeArray = mock( java.sql.Array.class );
		final Object[] elements = { 1, null, 3 };
		when( nativeArray.getArray() ).thenReturn( elements );
		final Object[] physical = { "B", nativeArray };
		final AggregateJdbcValueOrder order = AggregateJdbcValueOrder.physicalOrder( 1, 0 );
		assertArrayEquals( new Object[] { elements, "B" },
				AggregateJdbcValues.toLogicalJdbcValues( mapping, physical, order, options ) );
		assertEquals( Arrays.asList( elements, "B" ), AggregateJdbcValues.toDomainValue( mapping, physical, order, options ) );
		assertArrayEquals( new Object[] { "B", nativeArray }, physical );
		assertArrayEquals( new Object[] { 1, null, 3 }, elements );
	}

	@Test
	void domainConversionDoesNotMutateInputOrApplyConvertersTwice() throws SQLException {
		final EmbeddableMappingType mapping = mappingType( 1 );
		final JdbcMapping jdbcMapping = mapping.getAttributeMapping( 0 ).getSingleJdbcMapping();
		when( jdbcMapping.convertToDomainValue( any() ) ).thenAnswer( invocation -> "domain:" + invocation.getArgument( 0 ) );
		final Object[] physical = { "value" };
		assertEquals( List.of( "domain:value" ), AggregateJdbcValues.toDomainValue( mapping, physical, options ) );
		assertArrayEquals( new Object[] { "value" }, physical );
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

	private EmbeddableMappingType mappingType() throws SQLException {
		return mappingType( 3 );
	}

	@SuppressWarnings("unchecked")
	private EmbeddableMappingType mappingType(int attributeCount) throws SQLException {
		final EmbeddableMappingType mappingType = mock( EmbeddableMappingType.class );
		when( mappingType.getJdbcValueCount() ).thenReturn( attributeCount );
		when( mappingType.getNumberOfAttributeMappings() ).thenReturn( attributeCount );
		when( mappingType.getValues( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		for ( int i = 0; i < attributeCount; i++ ) {
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
				.thenAnswer( invocation -> Arrays.asList( invocation.getArgument( 0, org.hibernate.metamodel.spi.ValueAccess.class ).getValues() ) );
		return mappingType;
	}
}
