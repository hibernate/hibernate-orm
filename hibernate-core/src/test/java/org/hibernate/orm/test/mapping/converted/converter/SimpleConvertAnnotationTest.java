/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.net.MalformedURLException;
import java.net.URL;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * test handling of an AttributeConverter explicitly named via a @Convert annotation
 *
 * @author Steve Ebersole
 */
public class SimpleConvertAnnotationTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Entity1.class, UrlConverter.class, AutoUrlConverter.class };
	}

	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		final EntityPersister ep = sessionFactory().getMappingMetamodel().getEntityDescriptor(Entity1.class.getName());
		final Type websitePropertyType = ep.getPropertyType( "website" );
		final ConvertedBasicTypeImpl type = assertTyping(
				ConvertedBasicTypeImpl.class,
				websitePropertyType
		);
		final JpaAttributeConverter converter = (JpaAttributeConverter) type.getValueConverter();
		assertTrue( UrlConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );

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
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
		@Convert( converter = UrlConverter.class )
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
