/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.convert;

import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Tests MappedSuperclass/Entity overriding of Convert definitions
 *
 * @author Steve Ebersole
 */
public class SimpleEmbeddableOverriddenConverterTest extends BaseUnitTestCase {
	/**
	 * Test outcome of annotations exclusively.
	 */
	@Test
	public void testSimpleConvertOverrides() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Person.class.getName() );
			}
		};

		final Map settings = new HashMap();
//		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();

		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		try {
			final EntityPersister ep = sfi.getEntityPersister( Person.class.getName() );

			CompositeType homeAddressType = assertTyping( CompositeType.class, ep.getPropertyType( "homeAddress" ) );
			Type homeAddressCityType = findCompositeAttributeType( homeAddressType, "city" );
			assertTyping( StringType.class, homeAddressCityType );
		}
		finally {
			emf.close();
		}
	}

	public Type findCompositeAttributeType(CompositeType compositeType, String attributeName) {
		int pos = 0;
		for ( String name : compositeType.getPropertyNames() ) {
			if ( name.equals( attributeName ) ) {
				break;
			}
			pos++;
		}

		if ( pos >= compositeType.getPropertyNames().length ) {
			throw new IllegalStateException( "Could not locate attribute index for [" + attributeName + "] in composite" );
		}

		return compositeType.getSubtypes()[pos];
	}

	@Embeddable
	public static class Address {
		public String street;
		@Convert(converter = SillyStringConverter.class)
		public String city;
	}

	@Entity( name="Person" )
	public static class Person {
		@Id
		public Integer id;
		@Embedded
		@Convert( attributeName = "city", disableConversion = true )
		public Address homeAddress;
	}
}
