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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

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
