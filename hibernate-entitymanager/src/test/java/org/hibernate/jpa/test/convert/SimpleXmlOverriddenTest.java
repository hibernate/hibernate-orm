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

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * Test simple application of Convert annotation via XML.
 *
 * @author Steve Ebersole
 */
public class SimpleXmlOverriddenTest extends BaseUnitTestCase {
	@Test
	public void baseline() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Super.class.getName(), Sub.class.getName() );
			}

			// No mapping file should mean that the converter is applied
		};

		final Map settings = Collections.emptyMap();

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();

		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		try {
			final EntityPersister ep = sfi.getEntityPersister( Sub.class.getName() );

			Type type = ep.getPropertyType( "it" );
			try {
				assertTyping( StringType.class, type );
				fail( "Expected AttributeConverter to be applied" );
			}
			catch (AssertionError expected) {
			}
		}
		finally {
			emf.close();
		}
	}

	/**
	 * Test outcome of applying overrides via orm.xml, specifically at the entity level
	 */
	@Test
	public void testDefinitionAtEntityLevel() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Super.class.getName(), Sub.class.getName() );
			}

			@Override
			public List<String> getMappingFileNames() {
				return Arrays.asList( "org/hibernate/jpa/test/convert/simple-override.xml" );
			}
		};

		final Map settings = new HashMap();
//		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();

		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		try {
			final EntityPersister ep = sfi.getEntityPersister( Sub.class.getName() );

			Type type = ep.getPropertyType( "it" );
			assertTyping( StringType.class, type );
		}
		finally {
			emf.close();
		}
	}

	/**
	 * Test outcome of applying overrides via orm.xml, specifically at the entity level
	 */
	@Test
	public void testDefinitionAtAttributeLevel() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Super.class.getName(), Sub.class.getName() );
			}

			@Override
			public List<String> getMappingFileNames() {
				return Arrays.asList( "org/hibernate/jpa/test/convert/simple-override2.xml" );
			}
		};

		final Map settings = new HashMap();
//		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();

		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		try {
			final EntityPersister ep = sfi.getEntityPersister( Sub.class.getName() );

			Type type = ep.getPropertyType( "it" );
			assertTyping( StringType.class, type );
		}
		finally {
			emf.close();
		}
	}

	@MappedSuperclass
	public static class Super {
		@Id
		public Integer id;
		@Convert(converter = SillyStringConverter.class)
		public String it;
	}

	@Entity(name = "Sub")
	// the xml disabled conversion on the Sub#it attribute
	// Essentially the same test as org.hibernate.jpa.test.convert.SimpleOverriddenConverterTest, but through XML
	//@Convert( attributeName = "it", disableConversion = true )
	public static class Sub extends Super {
	}
}
