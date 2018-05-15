/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.fetchprofile;

import java.io.InputStream;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test case for HHH-4812
 *
 * @author Hardy Ferentschik
 */
@TestForIssue( jiraKey = "HHH-4812" )
public class FetchProfileTest extends BaseUnitTestCase {

	private ServiceRegistry serviceRegistry;

	@Before
    public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
    public void tearDown() {
        if (serviceRegistry != null) ServiceRegistryBuilder.destroy(serviceRegistry);
	}

	@Test
	public void testFetchProfileConfigured() {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Customer.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( SupportTickets.class );
		config.addAnnotatedClass( Country.class );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		);

		assertTrue(
				"fetch profile not parsed properly",
				sessionImpl.containsFetchProfileDefinition( "customer-with-orders" )
		);
		assertFalse(
				"package info should not be parsed",
				sessionImpl.containsFetchProfileDefinition( "package-profile-1" )
		);
		sessionImpl.close();
	}

	@Test
	public void testWrongAssociationName() {
		final MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( Customer2.class )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
            log.trace("success");
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Test
	public void testWrongClass() {
		final MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( Customer3.class )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
            log.trace("success");
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Test
	public void testUnsupportedFetchMode() {
		final MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( Customer4.class )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
            log.trace("success");
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Test
	public void testXmlOverride() {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Customer5.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( Country.class );
		InputStream is = Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream( "org/hibernate/test/annotations/fetchprofile/mappings.hbm.xml" );
		config.addInputStream( is );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		);

		assertTrue(
				"fetch profile not parsed properly",
				sessionImpl.containsFetchProfileDefinition( "orders-profile" )
		);
		sessionImpl.close();

		// now the same with no xml
		final MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( Customer5.class )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
			log.trace("success");
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Test
	public void testPackageConfiguredFetchProfile() {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Customer.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( SupportTickets.class );
		config.addAnnotatedClass( Country.class );
		config.addPackage( Customer.class.getPackage().getName() );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		);

		assertTrue(
				"fetch profile not parsed properly",
				sessionImpl.containsFetchProfileDefinition( "package-profile-1" )
		);
		assertTrue(
				"fetch profile not parsed properly",
				sessionImpl.containsFetchProfileDefinition( "package-profile-2" )
		);
		sessionImpl.close();
	}
}
