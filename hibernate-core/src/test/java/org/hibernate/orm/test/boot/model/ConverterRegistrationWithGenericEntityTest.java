/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Basic;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Reproducer for HHH-20276.
 * <p>
 * When a {@link jakarta.persistence.AttributeConverter} is registered via
 * {@link org.hibernate.annotations.ConverterRegistration} in a {@code package-info.java},
 * and an entity has a generic type parameter bounded by another class (e.g. {@code Book<T extends Person>}),
 * Hibernate throws an {@link IllegalArgumentException} during metamodel building with the message:
 * <pre>
 *     Book is not a subtype of Book
 * </pre>
 * The error originates from {@code GenericsHelper#typeArguments} which fails to resolve
 * the actual member type of the entity when the registered conversions map
 * ({@code AttributeConverterManager#registeredConversionsByDomainType}) is non-null —
 * a code path only triggered when converters are registered via {@code @ConverterRegistration},
 * and not when registered via {@code @Converter}.
 *
 * @author Vincent Bouthinon
 * @see org.hibernate.boot.model.convert.internal.AttributeConverterManager
 * @see org.hibernate.internal.util.GenericsHelper
 */
@Jpa(annotatedClasses = ConverterRegistrationWithGenericEntityTest.Book.class,
		annotatedPackageNames = "org.hibernate.orm.test.boot.model")

@JiraKey("HHH-20276")
class ConverterRegistrationWithGenericEntityTest {

	@Test
	void testLogEntityWithAnyKeyJavaClassAsString(EntityManagerFactoryScope scope) {
		assertThat(scope.getEntityManagerFactory()).isNotNull();
	}

	@Entity(name = "book")
	public static class Book<T extends Person> {

		@Id
		@GeneratedValue
		private Long id;

		@Basic
		private int isbn;
	}

	public static class Person {
	}

	@Converter
	public static class TestConverter implements AttributeConverter<String, String> {

		@Override
		public String convertToDatabaseColumn(String attribute) {
			return "";
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return "";
		}
	}
}
