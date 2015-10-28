/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import java.net.MalformedURLException;
import java.net.URL;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Converts;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * test handling of an AttributeConverter explicitly named via a @Converts annotation
 *
 * @author Steve Ebersole
 */
public class SimpleConvertsAnnotationTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Entity1.class, UrlConverter.class, AutoUrlConverter.class };
	}

	@Test
	public void testSimpleConvertsUsage() throws MalformedURLException {
		final EntityPersister ep = sessionFactory().getEntityPersister( Entity1.class.getName() );
		final Type websitePropertyType = ep.getPropertyType( "website" );
		final AttributeConverterTypeAdapter type = assertTyping(
				AttributeConverterTypeAdapter.class,
				websitePropertyType
		);
		assertTyping( UrlConverter.class, type.getAttributeConverter() );

		resetFlags();

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( new Entity1( 1, "1", new URL( "http://hibernate.org" ) ) );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToDatabaseColumnCalled );

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete Entity1" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	static boolean convertToDatabaseColumnCalled = false;
	static boolean convertToEntityAttributeCalled = false;

	private void resetFlags() {
		convertToDatabaseColumnCalled = false;
		convertToEntityAttributeCalled = false;
	}

	@Converter(autoApply = false)
	public static class UrlConverter implements AttributeConverter<URL,String> {
		@Override
		public String convertToDatabaseColumn(URL attribute) {
			convertToDatabaseColumnCalled = true;
			return attribute == null ? null : attribute.toExternalForm();
		}

		@Override
		public URL convertToEntityAttribute(String dbData) {
			convertToEntityAttributeCalled = true;
			if ( dbData == null ) {
				return null;
			}

			try {
				return new URL( dbData );
			}
			catch (MalformedURLException e) {
				throw new IllegalArgumentException( "Could not convert incoming value to URL : " + dbData );
			}
		}
	}

	@Converter( autoApply = true )
	public static class AutoUrlConverter implements AttributeConverter<URL,String> {
		@Override
		public String convertToDatabaseColumn(URL attribute) {
			throw new IllegalStateException( "Should not be called" );
		}

		@Override
		public URL convertToEntityAttribute(String dbData) {
			throw new IllegalStateException( "Should not be called" );
		}
	}

	@Entity( name = "Entity1" )
	@Converts(
			@Convert( attributeName = "website", converter = UrlConverter.class )
	)
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
		private URL website;

		public Entity1() {
		}

		public Entity1(Integer id, String name, URL website) {
			this.id = id;
			this.name = name;
			this.website = website;
		}
	}
}
