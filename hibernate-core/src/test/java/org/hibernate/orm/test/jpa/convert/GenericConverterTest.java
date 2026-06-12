/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.convert;

import jakarta.persistence.AttributeConverter;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author Yanming Zhou
 */
class GenericConverterTest {

	@JiraKey( "HHH-20467" )
	@Test
	void extendsFromAbstractConverterWithUnboundGenerics() {
		assertDoesNotThrow( () -> {
			try ( SessionFactory sf = new MetadataSources( new StandardServiceRegistryBuilder().build() )
					.addAnnotatedClass( StringConverter.class )
					.buildMetadata()
					.buildSessionFactory() ) {
				// SessionFactory creation is the only step required to reproduce the error.
			}
		} );
	}

	static class StringConverter<T> extends AbstractMiddleConverter<String> {

		@Override
		public String convertToDatabaseColumn(String attribute) {
			return "";
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return "";
		}
	}

	abstract static class AbstractMiddleConverter<T> extends AbstractBaseConverter<T> {

	}

	abstract static class AbstractBaseConverter<T> implements AttributeConverter<T, String> {

	}
}
