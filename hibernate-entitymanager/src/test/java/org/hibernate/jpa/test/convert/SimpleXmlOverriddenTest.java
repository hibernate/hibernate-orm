/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.convert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Test simple application of Convert annotation via XML.
 *
 * @author Steve Ebersole
 */
public class SimpleXmlOverriddenTest extends BaseUnitTestCase {
	/**
	 * A baseline test, with an explicit @Convert annotation that should be in effect
	 */
	@Test
	public void baseline() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( TheEntity.class.getName() );
			}

			// No mapping file should mean that the converter is applied
		};

		final Map settings = Collections.emptyMap();

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();

		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		try {
			final EntityPersister ep = sfi.getEntityPersister( TheEntity.class.getName() );

			Type type = ep.getPropertyType( "it" );
			AttributeConverterTypeAdapter adapter = assertTyping( AttributeConverterTypeAdapter.class, type );
			assertTyping( SillyStringConverter.class, adapter.getAttributeConverter() );
		}
		finally {
			emf.close();
		}
	}

	/**
	 * Test outcome of applying overrides via orm.xml, specifically at the attribute level
	 */
	@Test
	public void testDefinitionAtAttributeLevel() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( TheEntity.class.getName() );
			}

			@Override
			public List<String> getMappingFileNames() {
				return Arrays.asList( "org/hibernate/jpa/test/convert/simple-override.xml" );
			}
		};

		final Map settings = new HashMap();
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();

		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		try {
			final EntityPersister ep = sfi.getEntityPersister( TheEntity.class.getName() );

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
	public void testDefinitionAtEntityLevel() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( TheEntity2.class.getName() );
			}

			@Override
			public List<String> getMappingFileNames() {
				return Arrays.asList( "org/hibernate/jpa/test/convert/simple-override2.xml" );
			}
		};

		final Map settings = new HashMap();
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();

		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		try {
			final EntityPersister ep = sfi.getEntityPersister( TheEntity2.class.getName() );

			Type type = ep.getPropertyType( "it" );
			assertTyping( StringType.class, type );
		}
		finally {
			emf.close();
		}
	}

	@Entity(name="TheEntity")
	public static class TheEntity {
		@Id
		public Integer id;
		@Convert(converter = SillyStringConverter.class)
		public String it;
	}

	@Entity(name="TheEntity2")
	@Convert( attributeName = "it", converter = SillyStringConverter.class )
	public static class TheEntity2 {
		@Id
		public Integer id;
		public String it;
	}
}
