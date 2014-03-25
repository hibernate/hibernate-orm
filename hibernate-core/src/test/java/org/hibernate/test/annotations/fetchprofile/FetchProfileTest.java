/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.fetchprofile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for HHH-4812
 *
 * @author Hardy Ferentschik
 */
@TestForIssue( jiraKey = "HHH-4812" )
public class FetchProfileTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( FetchProfileTest.class );

	private BootstrapServiceRegistry bootstrapServiceRegistry;
	private StandardServiceRegistry serviceRegistry;
	
	@Before
	public void setUp() {
		bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();
		serviceRegistry = new StandardServiceRegistryBuilder( bootstrapServiceRegistry ).build();
	}
	
	@After
	public void tearDown() {
		( (ServiceRegistryImplementor) serviceRegistry ).destroy();
		( (ServiceRegistryImplementor) bootstrapServiceRegistry ).destroy();
	}
	

	@Test
	public void testFetchProfileConfigured() {
		MetadataSources metadataSources = new MetadataSources( bootstrapServiceRegistry );
		metadataSources.addAnnotatedClass( Customer.class );
		metadataSources.addAnnotatedClass( Order.class );
		metadataSources.addAnnotatedClass( SupportTickets.class );
		metadataSources.addAnnotatedClass( Country.class );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) metadataSources
				.getMetadataBuilder( serviceRegistry ).build().buildSessionFactory();

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
		MetadataSources metadataSources = new MetadataSources( bootstrapServiceRegistry );
		metadataSources.addAnnotatedClass( Customer2.class );
		metadataSources.addAnnotatedClass( Order.class );
		metadataSources.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
            log.trace("success");
		}
	}

	@Test
	public void testWrongClass() {
		MetadataSources metadataSources = new MetadataSources( bootstrapServiceRegistry );
		metadataSources.addAnnotatedClass( Customer3.class );
		metadataSources.addAnnotatedClass( Order.class );
		metadataSources.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
            log.trace("success");
		}
	}

	@Test
	public void testUnsupportedFetchMode() {
		MetadataSources metadataSources = new MetadataSources( bootstrapServiceRegistry );
		metadataSources.addAnnotatedClass( Customer4.class );
		metadataSources.addAnnotatedClass( Order.class );
		metadataSources.addAnnotatedClass( Country.class );

		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
            log.trace("success");
		}
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testXmlOverride() {
		MetadataSources metadataSources = new MetadataSources( bootstrapServiceRegistry );
		metadataSources.addAnnotatedClass( Customer5.class );
		metadataSources.addAnnotatedClass( Order.class );
		metadataSources.addAnnotatedClass( Country.class );
		InputStream is = Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream( "org/hibernate/test/annotations/fetchprofile/mappings.hbm.xml" );
		metadataSources.addInputStream( is );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) metadataSources
				.getMetadataBuilder( serviceRegistry ).build().buildSessionFactory();

		assertTrue(
				"fetch profile not parsed properly",
				sessionImpl.containsFetchProfileDefinition( "orders-profile" )
		);
		sessionImpl.close();

		// now the same with no xml
		metadataSources = new MetadataSources( bootstrapServiceRegistry );
		metadataSources.addAnnotatedClass( Customer5.class );
		metadataSources.addAnnotatedClass( Order.class );
		metadataSources.addAnnotatedClass( Country.class );
		try {
			metadataSources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
            log.trace("success");
		}
	}

	@Test
	public void testPackageConfiguredFetchProfile() {
		MetadataSources metadataSources = new MetadataSources( bootstrapServiceRegistry );
		metadataSources.addAnnotatedClass( Customer.class );
		metadataSources.addAnnotatedClass( Order.class );
		metadataSources.addAnnotatedClass( SupportTickets.class );
		metadataSources.addAnnotatedClass( Country.class );
		metadataSources.addPackage( Customer.class.getPackage().getName() );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) metadataSources
				.getMetadataBuilder( serviceRegistry ).build().buildSessionFactory();

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
