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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * Tests for asserting correct behavior of applying AttributeConverters explicitly listed in persistence.xml.
 *
 * @author Steve Ebersole
 */
public class ExplicitlyNamedConverterClassesTest extends BaseUnitTestCase {

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
	public void testNonAutoAppliedConvertIsNotApplied() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Entity1.class.getName(), NotAutoAppliedConverter.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			em.persist( new Entity1( 1, "1" ) );
			em.getTransaction().commit();
			em.close();

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
