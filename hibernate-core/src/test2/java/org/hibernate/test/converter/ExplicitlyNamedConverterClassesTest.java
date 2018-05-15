/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;

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
