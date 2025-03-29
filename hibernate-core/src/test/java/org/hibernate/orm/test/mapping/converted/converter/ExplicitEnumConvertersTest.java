/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.net.MalformedURLException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-8809" )
public class ExplicitEnumConvertersTest extends BaseNonConfigCoreFunctionalTestCase {

	// NOTE : initially unable to reproduce the reported problem

	public static enum MediaType {
		MUSIC,
		VIDEO,
		PHOTO,
		MUSIC_STREAM,
		VIDEO_STREAM
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Entity1.class };
	}


	static boolean convertToDatabaseColumnCalled = false;
	static boolean convertToEntityAttributeCalled = false;

	private void resetFlags() {
		convertToDatabaseColumnCalled = false;
		convertToEntityAttributeCalled = false;
	}

	public static class MediaTypeConverter implements AttributeConverter<MediaType,String> {
		@Override
		public String convertToDatabaseColumn(MediaType attribute) {
			convertToDatabaseColumnCalled = true;
			return attribute.name();
		}

		@Override
		public MediaType convertToEntityAttribute(String dbData) {
			convertToEntityAttributeCalled = true;
			return MediaType.valueOf( dbData );
		}
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
		@Convert( converter = MediaTypeConverter.class )
		private MediaType mediaType;

		public Entity1() {
		}

		public Entity1(Integer id, String name, MediaType mediaType) {
			this.id = id;
			this.name = name;
			this.mediaType = mediaType;
		}
	}

	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		final EntityPersister ep = sessionFactory().getMappingMetamodel().getEntityDescriptor(Entity1.class.getName());
		final Type theDatePropertyType = ep.getPropertyType( "mediaType" );
		final ConvertedBasicTypeImpl type = assertTyping(
				ConvertedBasicTypeImpl.class,
				theDatePropertyType
		);
		final JpaAttributeConverter converter = (JpaAttributeConverter) type.getValueConverter();
		assertTrue( MediaTypeConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );

		resetFlags();

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( new Entity1( 1, "300", MediaType.VIDEO ) );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToDatabaseColumnCalled );
		resetFlags();

		session = openSession();
		session.getTransaction().begin();
		session.get( Entity1.class, 1 );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToEntityAttributeCalled );

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete Entity1" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
