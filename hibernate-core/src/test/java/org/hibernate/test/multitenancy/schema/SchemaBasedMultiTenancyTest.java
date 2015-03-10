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
package org.hibernate.test.multitenancy.schema;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.TargetDatabaseImpl;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.common.JdbcConnectionAccessImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature( value = ConnectionProviderBuilder.class )
public class SchemaBasedMultiTenancyTest extends BaseUnitTestCase {
	private DriverManagerConnectionProviderImpl acmeProvider;
	private DriverManagerConnectionProviderImpl jbossProvider;

	private ServiceRegistryImplementor serviceRegistry;

	protected SessionFactoryImplementor sessionFactory;

	@Before
	public void setUp() {
		AbstractMultiTenantConnectionProvider multiTenantConnectionProvider = buildMultiTenantConnectionProvider();

		Map settings = new HashMap();
		settings.put( Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA );
		settings.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		settings.put( Environment.GENERATE_STATISTICS, "true" );

		serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
				.applySettings( settings )
				.addService( MultiTenantConnectionProvider.class, multiTenantConnectionProvider )
				.build();

		MetadataSources ms = new MetadataSources( serviceRegistry );
		ms.addAnnotatedClass( Customer.class );
		ms.addAnnotatedClass( Invoice.class );

		Metadata metadata = ms.buildMetadata();
		( (RootClass) metadata.getEntityBinding( Customer.class.getName() ) ).setCacheConcurrencyStrategy( "read-write" );

		final TargetDatabaseImpl acmeTarget = new TargetDatabaseImpl( new JdbcConnectionAccessImpl( acmeProvider ) );
		final TargetDatabaseImpl jbossTarget = new TargetDatabaseImpl( new JdbcConnectionAccessImpl( jbossProvider ) );

		serviceRegistry.getService( SchemaManagementTool.class ).getSchemaDropper( settings ).doDrop(
				metadata,
				true,
				acmeTarget,
				jbossTarget
		);

		serviceRegistry.getService( SchemaManagementTool.class ).getSchemaCreator( settings ).doCreation(
				metadata,
				true,
				acmeTarget,
				jbossTarget
		);

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		configure( sfb );
		sessionFactory = (SessionFactoryImplementor) sfb.build();
	}

	protected void configure(SessionFactoryBuilder sfb) {
	}

	private AbstractMultiTenantConnectionProvider buildMultiTenantConnectionProvider() {
		acmeProvider = ConnectionProviderBuilder.buildConnectionProvider( "acme" );
		jbossProvider = ConnectionProviderBuilder.buildConnectionProvider( "jboss" );
		return new AbstractMultiTenantConnectionProvider() {
			@Override
			protected ConnectionProvider getAnyConnectionProvider() {
				return acmeProvider;
			}

			@Override
			protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
				if ( "acme".equals( tenantIdentifier ) ) {
					return acmeProvider;
				}
				else if ( "jboss".equals( tenantIdentifier ) ) {
					return jbossProvider;
				}
				throw new HibernateException( "Unknown tenant identifier" );
			}
		};
	}

	@After
	public void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
		if ( serviceRegistry != null ) {
			serviceRegistry.destroy();
		}
		if ( jbossProvider != null ) {
			jbossProvider.stop();
		}
		if ( acmeProvider != null ) {
			acmeProvider.stop();
		}
	}

	@Test
	public void testBasicExpectedBehavior() {
		Session session = getNewSession("jboss");
		session.beginTransaction();
		Customer steve = new Customer( 1L, "steve" );
		session.save( steve );
		session.getTransaction().commit();
		session.close();

		session = getNewSession("acme");
		try {
			session.beginTransaction();
			Customer check = (Customer) session.get( Customer.class, steve.getId() );
			Assert.assertNull( "tenancy not properly isolated", check );
		}
		finally {
			session.getTransaction().commit();
			session.close();
		}

		session = getNewSession("jboss");
		session.beginTransaction();
		session.delete( steve );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testSameIdentifiers() {
		// create a customer 'steve' in jboss
		Session session = getNewSession("jboss");
		session.beginTransaction();
		Customer steve = new Customer( 1L, "steve" );
		session.save( steve );
		session.getTransaction().commit();
		session.close();

		// now, create a customer 'john' in acme
		session = getNewSession("acme");
		session.beginTransaction();
		Customer john = new Customer( 1L, "john" );
		session.save( john );
		session.getTransaction().commit();
		session.close();

		sessionFactory.getStatisticsImplementor().clear();

		// make sure we get the correct people back, from cache
		// first, jboss
		{
			session = getNewSession("jboss");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 1, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}
		sessionFactory.getStatisticsImplementor().clear();
		// then, acme
		{
			session = getNewSession("acme");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "john", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 1, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}

		// make sure the same works from datastore too
		sessionFactory.getStatisticsImplementor().clear();
		sessionFactory.getCache().evictEntityRegions();
		// first jboss
		{
			session = getNewSession("jboss");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 0, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}
		sessionFactory.getStatisticsImplementor().clear();
		// then, acme
		{
			session = getNewSession("acme");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "john", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 0, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}

		session = getNewSession("jboss");
		session.beginTransaction();
		session.delete( steve );
		session.getTransaction().commit();
		session.close();

		session = getNewSession("acme");
		session.beginTransaction();
		session.delete( john );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testTableIdentifiers() {
		Session session = getNewSession( "jboss" );
		session.beginTransaction();
		Invoice orderJboss = new Invoice();
		session.save( orderJboss );
		Assert.assertEquals( Long.valueOf( 1 ), orderJboss.getId() );
		session.getTransaction().commit();
		session.close();

		session = getNewSession( "acme" );
		session.beginTransaction();
		Invoice orderAcme = new Invoice();
		session.save( orderAcme );
		Assert.assertEquals( Long.valueOf( 1 ), orderAcme.getId() );
		session.getTransaction().commit();
		session.close();

		session = getNewSession( "jboss" );
		session.beginTransaction();
		session.delete( orderJboss );
		session.getTransaction().commit();
		session.close();

		session = getNewSession( "acme" );
		session.beginTransaction();
		session.delete( orderAcme );
		session.getTransaction().commit();
		session.close();

		sessionFactory.getStatisticsImplementor().clear();
	}

	protected Session getNewSession(String tenant) {
		return sessionFactory.withOptions().tenantIdentifier( tenant ).openSession();
	}

}
