/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// An array of structured elements, where the struct itself contains an array
/// component, converted through [ArrayJdbcType#getArray] with a
/// [StructuredJdbcType] that honors the provider contract: its
/// [AggregateJdbcType#extractJdbcValues] returns logical values, as both the
/// stock [StructJdbcType] and the provider fixture's example do.
///
/// [ArrayJdbcType#toJavaArray] feeds that logical output back into
/// [StructHelper#getAttributeValues], which expects physical values, so the
/// array component is wrapped a second time and the
/// `java.sql.Array` cast fails.
///
/// @author Jeff Yemin
class ArrayJdbcTypeStructuredElementTest {
	private final WrapperOptions options = mock( WrapperOptions.class );

	@Test
	void convertsStructuredElementsContainingArrays() throws SQLException {
		final StructJdbcType structJdbcType = new StructJdbcType( structMapping(), "test_struct", null );
		final ExposedArrayJdbcType arrayJdbcType = new ExposedArrayJdbcType( structJdbcType );

		// one struct element, in the raw driver shapes a real driver produces: a java.sql.Struct
		// whose single attribute is a java.sql.Array. The element JdbcType is stubbed with
		// TIMESTAMP_UTC to reach wrapRawJdbcArray's temporal branch (the one that casts to
		// java.sql.Array); the values stay integers because the JavaType wraps are identity mocks
		final Object[] structPhysicalValues = { elementArray( 1, 2 ) };
		final java.sql.Array arrayOfStructs = arrayOf( new Object[]{ struct( structPhysicalValues ) } );

		final BasicExtractor<?> extractor = mock( BasicExtractor.class );
		final JavaType<Object> arrayJavaType = mock( JavaType.class );
		doReturn( arrayJavaType ).when( extractor ).getJavaType();
		when( arrayJavaType.wrap( any(), any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		final Object result = arrayJdbcType.extractArray( extractor, arrayOfStructs, options );

		final Object[] domainArray = (Object[]) result;
		assertEquals( 1, domainArray.length );
		final List<?> structValue = (List<?>) domainArray[0];
		assertArrayEquals( new Object[] { 1, 2 }, (Object[]) structValue.get( 0 ) );
	}

	@Test
	void convertsStructuredElementsContainingDirectJavaTimeArrays() throws SQLException {
		final ArrayJavaType<LocalDateTime> directArrayJavaType = spy(
				new ArrayJavaType<>( LocalDateTimeJavaType.INSTANCE )
		);
		final StructJdbcType structJdbcType = new StructJdbcType(
				directJavaTimeStructMapping( directArrayJavaType ),
				"test_struct",
				null
		);
		final ExposedArrayJdbcType arrayJdbcType = new ExposedArrayJdbcType( structJdbcType );
		final LocalDateTime first = LocalDateTime.of( 2024, 2, 29, 12, 34, 56 );
		final LocalDateTime second = first.plusDays( 1 );
		final Object[] structPhysicalValues = { arrayOf( new Object[] { first, second } ) };
		final java.sql.Array arrayOfStructs = arrayOf( new Object[] { struct( structPhysicalValues ) } );

		final BasicExtractor<?> extractor = mock( BasicExtractor.class );
		final JavaType<Object> arrayJavaType = mock( JavaType.class );
		doReturn( arrayJavaType ).when( extractor ).getJavaType();
		when( arrayJavaType.wrap( any(), any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		final Object result = arrayJdbcType.extractArray( extractor, arrayOfStructs, options );

		final Object[] domainArray = (Object[]) result;
		assertEquals( 1, domainArray.length );
		final List<?> structValue = (List<?>) domainArray[0];
		assertArrayEquals( new LocalDateTime[] { first, second }, (Object[]) structValue.get( 0 ) );
		verify( directArrayJavaType, times( 1 ) ).wrap( any(), any() );
	}

	@SuppressWarnings("unchecked")
	private EmbeddableMappingType structMapping() throws SQLException {
		final JavaType<Object> arrayJdbcJavaType = mock( JavaType.class );
		final ValueBinder<Object> arrayValueBinder = mock( ValueBinder.class );
		final JavaType<Object> elementJdbcJavaType = mock( JavaType.class );
		final JdbcType elementJdbcType = mock( JdbcType.class );
		when( elementJdbcType.getDefaultSqlTypeCode() ).thenReturn( SqlTypes.TIMESTAMP_UTC );
		when( elementJdbcJavaType.wrap( any(), any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		when( arrayJdbcJavaType.wrap( any(), any() ) ).thenAnswer( invocation -> {
			final Object value = invocation.getArgument( 0 );
			return value instanceof java.sql.Array array ? array.getArray() : value;
		} );
		when( arrayJdbcJavaType.isInstance( any() ) ).thenReturn( true );
		when( arrayJdbcJavaType.cast( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		when( arrayValueBinder.getBindValue( any(), any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		return structMapping( elementJdbcType, elementJdbcJavaType, arrayJdbcJavaType, arrayValueBinder );
	}

	@SuppressWarnings("unchecked")
	private EmbeddableMappingType directJavaTimeStructMapping(JavaType<?> arrayJdbcJavaType) throws SQLException {
		return structMapping(
				LocalDateTimeJdbcType.INSTANCE,
				LocalDateTimeJavaType.INSTANCE,
				arrayJdbcJavaType,
				mock( ValueBinder.class )
		);
	}

	@SuppressWarnings("unchecked")
	private EmbeddableMappingType structMapping(
			JdbcType elementJdbcType,
			JavaType<?> elementJdbcJavaType,
			JavaType<?> arrayJdbcJavaType,
			ValueBinder<?> arrayValueBinder) {
		final EmbeddableMappingType mappingType = mock( EmbeddableMappingType.class );
		when( mappingType.getJdbcValueCount() ).thenReturn( 1 );
		when( mappingType.getNumberOfAttributeMappings() ).thenReturn( 1 );
		when( mappingType.getValues( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		// the struct's single attribute is an array; its mapping is a plural type whose
		// wrap unwraps a java.sql.Array, exactly as the array JavaTypes do
		final AttributeMapping attributeMapping = mock( AttributeMapping.class );
		final BasicPluralType<Object, Object> arrayMapping = mock( BasicPluralType.class );
		final JdbcType arrayJdbcType = mock( JdbcType.class );
		final BasicType<Object> elementType = mock( BasicType.class );

		when( mappingType.getAttributeMapping( 0 ) ).thenReturn( attributeMapping );
		when( attributeMapping.getJdbcTypeCount() ).thenReturn( 1 );
		when( attributeMapping.getSingleJdbcMapping() ).thenReturn( (JdbcMapping) arrayMapping );
		when( arrayMapping.getJdbcType() ).thenReturn( arrayJdbcType );
		when( arrayJdbcType.getJdbcTypeCode() ).thenReturn( SqlTypes.ARRAY );
		when( arrayJdbcType.getDefaultSqlTypeCode() ).thenReturn( SqlTypes.ARRAY );
		when( arrayMapping.getElementType() ).thenReturn( elementType );
		doReturn( elementJdbcType ).when( elementType ).getJdbcType();
		doReturn( elementJdbcJavaType ).when( elementType ).getJdbcJavaType();
		when( arrayMapping.convertToRelationalValue( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		when( arrayMapping.convertToDomainValue( any() ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
		doReturn( arrayJdbcJavaType ).when( arrayMapping ).getJdbcJavaType();
		doReturn( arrayValueBinder ).when( arrayMapping ).getJdbcValueBinder();

		final EmbeddableRepresentationStrategy representationStrategy = mock( EmbeddableRepresentationStrategy.class );
		final EmbeddableInstantiator instantiator = mock( EmbeddableInstantiator.class );
		when( mappingType.getRepresentationStrategy() ).thenReturn( representationStrategy );
		when( representationStrategy.getInstantiator() ).thenReturn( instantiator );
		when( instantiator.instantiate( any() ) ).thenAnswer(
				invocation -> asList( invocation.getArgument( 0, org.hibernate.metamodel.spi.ValueAccess.class ).getValues() ) );
		return mappingType;
	}

	private static java.sql.Array arrayOf(Object[] elements) throws SQLException {
		final java.sql.Array array = mock( java.sql.Array.class );
		when( array.getArray() ).thenReturn( elements );
		return array;
	}

	private static java.sql.Array elementArray(Integer... elements) throws SQLException {
		return arrayOf( elements );
	}

	private static java.sql.Struct struct(Object[] attributeValues) {
		final java.sql.Struct struct = mock( java.sql.Struct.class );
		try {
			when( struct.getAttributes() ).thenReturn( attributeValues );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
		return struct;
	}

	/**
	 * Exposes the protected {@link ArrayJdbcType#getArray} for the test.
	 */
	private static class ExposedArrayJdbcType extends ArrayJdbcType {
		private ExposedArrayJdbcType(JdbcType elementJdbcType) {
			super( elementJdbcType );
		}

		<X> X extractArray(BasicExtractor<X> extractor, java.sql.Array array, WrapperOptions options)
				throws SQLException {
			return getArray( extractor, array, options );
		}
	}
}
