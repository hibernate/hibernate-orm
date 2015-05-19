/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.convert;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-8809" )
public class ExplicitEnumConvertersTest {

	// NOTE : initially unable to reproduce the reported problem

	public static enum MediaType {
		MUSIC,
		VIDEO,
		PHOTO,
		MUSIC_STREAM,
		VIDEO_STREAM
	}

	public static class MediaTypeConverter implements AttributeConverter<MediaType,String> {
		@Override
		public String convertToDatabaseColumn(MediaType attribute) {
			callsToConverter++;
			return attribute.name();
		}

		@Override
		public MediaType convertToEntityAttribute(String dbData) {
			callsToConverter++;
			return MediaType.valueOf( dbData );
		}
	}

	static int callsToConverter = 0;

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
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Entity1.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		final EntityPersister ep = emf.unwrap( SessionFactoryImplementor.class ).getEntityPersister( Entity1.class.getName() );
		final Type theDatePropertyType = ep.getPropertyType( "mediaType" );
		final AttributeConverterTypeAdapter type = assertTyping( AttributeConverterTypeAdapter.class, theDatePropertyType );
		assertTyping( MediaTypeConverter.class, type.getAttributeConverter() );

		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			em.persist( new Entity1( 1, "300", MediaType.VIDEO ) );
			em.getTransaction().commit();
			em.close();

			assertEquals( 1, callsToConverter );

			em = emf.createEntityManager();
			em.getTransaction().begin();
			em.find( Entity1.class, 1 );
			em.getTransaction().commit();
			em.close();

			assertEquals( 2, callsToConverter );

			em = emf.createEntityManager();
			em.getTransaction().begin();
			em.createQuery( "delete Entity1" ).executeUpdate();
			em.getTransaction().commit();
			em.close();
		}
		finally {
			emf.close();
		}
	}
}
