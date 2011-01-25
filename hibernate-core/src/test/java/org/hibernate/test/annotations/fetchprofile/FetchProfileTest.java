// $Id$
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.annotations.fetchprofile;

import static org.hibernate.TestLogger.LOG;
import java.io.InputStream;
import junit.framework.TestCase;
import org.hibernate.MappingException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.test.common.ServiceRegistryHolder;

/**
 * Test case for HHH-4812
 *
 * @author Hardy Ferentschik
 */
public class FetchProfileTest extends TestCase {

	private ServiceRegistryHolder serviceRegistryHolder;

	@Override
    protected void setUp() {
		serviceRegistryHolder = new ServiceRegistryHolder( Environment.getProperties() );
	}

	@Override
    protected void tearDown() {
		if ( serviceRegistryHolder != null ) {
			serviceRegistryHolder.destroy();
		}
	}

	public void testFetchProfileConfigured() {
		AnnotationConfiguration config = new AnnotationConfiguration();
		config.addAnnotatedClass( Customer.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( SupportTickets.class );
		config.addAnnotatedClass( Country.class );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistryHolder.getServiceRegistry()
		);

		assertTrue(
				"fetch profile not parsed properly",
				sessionImpl.containsFetchProfileDefinition( "customer-with-orders" )
		);
		assertFalse(
				"package info should not be parsed",
				sessionImpl.containsFetchProfileDefinition( "package-profile-1" )
		);
	}

	public void testWrongAssociationName() {
		AnnotationConfiguration config = new AnnotationConfiguration();
		config.addAnnotatedClass( Customer2.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( Country.class );

		try {
			config.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
			fail();
		}
		catch ( MappingException e ) {
            LOG.trace("success");
		}
	}

	public void testWrongClass() {
		AnnotationConfiguration config = new AnnotationConfiguration();
		config.addAnnotatedClass( Customer3.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( Country.class );

		try {
			config.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
			fail();
		}
		catch ( MappingException e ) {
            LOG.trace("success");
		}
	}

	public void testUnsupportedFetchMode() {
		AnnotationConfiguration config = new AnnotationConfiguration();
		config.addAnnotatedClass( Customer4.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( Country.class );

		try {
			config.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
			fail();
		}
		catch ( MappingException e ) {
            LOG.trace("success");
		}
	}

	public void testXmlOverride() {
		AnnotationConfiguration config = new AnnotationConfiguration();
		config.addAnnotatedClass( Customer5.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( Country.class );
		InputStream is = Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream( "org/hibernate/test/annotations/fetchprofile/mappings.hbm.xml" );
		config.addInputStream( is );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistryHolder.getServiceRegistry()
		);

		assertTrue(
				"fetch profile not parsed properly",
				sessionImpl.containsFetchProfileDefinition( "orders-profile" )
		);

		// now the same with no xml
		config = new AnnotationConfiguration();
		config.addAnnotatedClass( Customer5.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( Country.class );
		try {
			config.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
			fail();
		}
		catch ( MappingException e ) {
            LOG.trace("success");
		}
	}

	public void testPackageConfiguredFetchProfile() {
		AnnotationConfiguration config = new AnnotationConfiguration();
		config.addAnnotatedClass( Customer.class );
		config.addAnnotatedClass( Order.class );
		config.addAnnotatedClass( SupportTickets.class );
		config.addAnnotatedClass( Country.class );
		config.addPackage( Customer.class.getPackage().getName() );
		SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistryHolder.getServiceRegistry()
		);

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