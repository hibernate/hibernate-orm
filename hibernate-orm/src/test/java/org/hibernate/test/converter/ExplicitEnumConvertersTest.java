/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import java.net.MalformedURLException;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-8809" )
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
		final EntityPersister ep = sessionFactory().getEntityPersister( Entity1.class.getName() );
		final Type theDatePropertyType = ep.getPropertyType( "mediaType" );
		final AttributeConverterTypeAdapter type = assertTyping(
				AttributeConverterTypeAdapter.class,
				theDatePropertyType
		);
		assertTyping( MediaTypeConverter.class, type.getAttributeConverter() );

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
