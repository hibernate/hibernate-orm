/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Tests for asserting correct behavior of applying AttributeConverters explicitly listed in persistence.xml.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {ExplicitlyNamedConverterClassesTest.Entity1.class, ExplicitlyNamedConverterClassesTest.NotAutoAppliedConverter.class}
)
@SessionFactory()
public class ExplicitlyNamedConverterClassesTest {

	// test handling of explicitly named, but non-auto-applied converter ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Converter(autoApply = false)
	public static class NotAutoAppliedConverter implements AttributeConverter<String,String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			throw new IllegalStateException( "AttributeConverter should not have been applied/called" );
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			throw new IllegalStateException( "AttributeConverter should not have been applied/called" );
		}
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;

		public Entity1() {
		}

		public Entity1(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Test
	public void testNonAutoAppliedConvertIsNotApplied(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(new Entity1(1, "1")) );

		scope.dropData();
	}
}
