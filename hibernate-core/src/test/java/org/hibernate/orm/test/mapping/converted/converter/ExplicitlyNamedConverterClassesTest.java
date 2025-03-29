/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests for asserting correct behavior of applying AttributeConverters explicitly listed in persistence.xml.
 *
 * @author Steve Ebersole
 */
public class ExplicitlyNamedConverterClassesTest extends BaseNonConfigCoreFunctionalTestCase {

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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Entity1.class, NotAutoAppliedConverter.class };
	}

	@Test
	public void testNonAutoAppliedConvertIsNotApplied() {
		Session session = openSession();
		session.getTransaction().begin();
		session.persist( new Entity1( 1, "1" ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete Entity1" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
