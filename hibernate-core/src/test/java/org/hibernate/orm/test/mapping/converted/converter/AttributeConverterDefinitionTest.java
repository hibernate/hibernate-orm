/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.hibernate.boot.model.convert.internal.GenericTypeResolver.erasedType;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey(value = "HHH-8854")
class AttributeConverterDefinitionTest {

	@MethodSource("arguments")
	@ParameterizedTest
	void test(Class<? extends AttributeConverter<?, ?>> attributeConverterClass, Class<?> expectedType) {
		final var converterDescriptor = ConverterDescriptors.of( attributeConverterClass );
		assertEquals( expectedType, erasedType( converterDescriptor.getDomainValueResolvedType() ) );
	}

	private static Stream<Arguments> arguments() {
		return Stream.of(
				Arguments.arguments( AttrConverterPlain.class, LocalDate.class ),
				Arguments.arguments( AttrConverterPlainExtended.class, LocalDate.class ),
				Arguments.arguments( AttrConverterPlainExtendedImplements.class, LocalDate.class ),
				Arguments.arguments( AttrConverterSameTypeImpl.class, String.class ),
				Arguments.arguments( DelegatingAttrConverterImpl.class, Integer.class ),
				Arguments.arguments( MultiAttrConverter.class, Integer.class ),
				Arguments.arguments( CollectionAttrConverter.class, Collection.class )
		);
	}

	public static class AttrConverterPlain implements AttributeConverter<LocalDate, String> {

		@Override
		public String convertToDatabaseColumn(LocalDate o) {
			return "";
		}

		@Override
		public LocalDate convertToEntityAttribute(String s) {
			return null;
		}
	}

	public static class AttrConverterPlainExtended extends AttrConverterPlain {
	}

	public static class AttrConverterPlainExtendedImplements extends AttrConverterPlain
			implements AttributeConverter<LocalDate, String> {
	}

	public interface AttrConverterSameType<T> extends AttributeConverter<T, T> {
	}

	public static class AttrConverterSameTypeImpl implements AttrConverterSameType<String> {

		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}

	public interface DelegatingAttributeConverter<T, V> extends AttributeConverter<T, V> {
		AttributeConverter<T, V> delegate();

		@Override
		default V convertToDatabaseColumn(T attribute) {
			return delegate().convertToDatabaseColumn( attribute );
		}

		@Override
		default T convertToEntityAttribute(V dbData) {
			return delegate().convertToEntityAttribute( dbData );
		}
	}

	public static abstract class DelegatingAttrConverter<T, V> implements DelegatingAttributeConverter<T, V> {
	}

	public static class DelegatingAttrConverterImpl implements DelegatingAttributeConverter<Integer, String> {
		@Override
		public AttributeConverter<Integer, String> delegate() {
			return null;
		}
	}

	public static class MultiAttrConverter extends DelegatingAttrConverter<Integer, Integer>
			implements AttrConverterSameType<Integer> {

		@Override
		public AttributeConverter<Integer, Integer> delegate() {
			return null;
		}
	}

	public static class CollectionAttrConverter implements AttributeConverter<Collection<String>, String> {

		@Override
		public String convertToDatabaseColumn(Collection<String> attribute) {
			return "";
		}

		@Override
		public Collection<String> convertToEntityAttribute(String dbData) {
			return List.of();
		}
	}

}
