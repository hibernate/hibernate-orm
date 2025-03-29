/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

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
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test case for HHH-4812
 *
 * @author Hardy Ferentschik
 */
@JiraKey( value = "HHH-4812" )
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
		try (SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		)) {

			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "customer-with-orders" )
			);
			assertFalse(
					"package info should not be parsed",
					sessionImpl.containsFetchProfileDefinition( "package-profile-1" )
			);
		}
	}

	@Test
	public void testWrongAssociationName() {
		final MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
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
		final MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
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
	public void testNowSupportedFetchMode() {
		final MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
				.addAnnotatedClass( Customer4.class )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
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
				.getResourceAsStream( "org/hibernate/orm/test/annotations/fetchprofile/mappings.hbm.xml" );
		config.addInputStream( is );
		try (SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		)) {

			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "orders-profile" )
			);
		}

		// now the same with no xml
		final MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
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
		try (SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		)) {

			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "package-profile-1" )
			);
			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "package-profile-2" )
			);
		}
	}
}
