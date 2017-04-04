/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.test.annotations.namingstrategy;

import java.util.Locale;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Test harness for ANN-716.
 *
 * @author Hardy Ferentschik
 */
public class NamingStrategyTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( NamingStrategyTest.class );

	private ServiceRegistry serviceRegistry;

	@Before
    public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
    public void tearDown() {
        if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
    @Test
	public void testWithCustomNamingStrategy() throws Exception {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass(Address.class)
				.addAnnotatedClass(Person.class)
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new DummyNamingStrategy() )
				.build();
	}

    @Test
	public void testWithJpaCompliantNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( AddressEntry.class )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE )
				.build();

		Collection collectionBinding = metadata.getCollectionBinding( A.class.getName() + ".address" );
		assertEquals(
				"Expecting A#address collection table name (implicit) to be [A_address] per JPA spec (section 11.1.8)",
				"A_ADDRESS",
				collectionBinding.getCollectionTable().getQuotedName().toUpperCase(Locale.ROOT)
		);
	}

    @Test
	public void testWithoutCustomNamingStrategy() throws Exception {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.buildMetadata();
	}
}
