/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.convert;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class SimpleConvertAnnotationTest extends BaseUnitTestCase {

	// test handling of an AttributeConverter explicitly named via a @Convert annotation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	@FailureExpectedWithNewMetamodel( jiraKey = "HHH-8932" )
	public void testSimpleConvertUsage() throws MalformedURLException {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Entity1.class.getName(), UrlConverter.class.getName(), AutoUrlConverter.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		final EntityPersister ep = emf.unwrap( SessionFactoryImplementor.class ).getEntityPersister( Entity1.class.getName() );
		final Type websitePropertyType = ep.getPropertyType( "website" );
		final AttributeConverterTypeAdapter type = assertTyping( AttributeConverterTypeAdapter.class, websitePropertyType );
		assertTyping( UrlConverter.class, type.getAttributeConverter() );

		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			em.persist( new Entity1( 1, "1", new URL( "http://hibernate.org" ) ) );
			em.getTransaction().commit();
			em.close();

			assertEquals( 1, callsToConverter );

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

	static int callsToConverter = 0;

	@Converter(autoApply = false)
	public static class UrlConverter implements AttributeConverter<URL,String> {
		@Override
		public String convertToDatabaseColumn(URL attribute) {
			callsToConverter++;
			return attribute == null ? null : attribute.toExternalForm();
		}

		@Override
		public URL convertToEntityAttribute(String dbData) {
			callsToConverter++;
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
