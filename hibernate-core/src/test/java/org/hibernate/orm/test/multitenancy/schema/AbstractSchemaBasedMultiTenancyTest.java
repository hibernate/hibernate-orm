/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.SessionBuilder;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.ExtractedDatabaseMetaDataImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernateSessionBuilder;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSchemaBasedMultiTenancyTest<T extends MultiTenantConnectionProvider<String>, C extends ConnectionProvider> extends BaseUnitTestCase {
	protected C acmeProvider;
	protected C jbossProvider;

	protected ServiceRegistryImplementor serviceRegistry;

	protected SessionFactoryImplementor sessionFactory;

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Before
	public void setUp() {
		T multiTenantConnectionProvider = buildMultiTenantConnectionProvider();

		Map<String,Object> settings = new HashMap<>();
		settings.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		settings.put( Environment.GENERATE_STATISTICS, "true" );

		serviceRegistry = (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( settings )
				// Make sure to continue configuring the MultiTenantConnectionProvider by adding a service,
				// rather than by setting 'hibernate.multi_tenant_connection_provider':
				// that's important to reproduce the regression we test in 'testJdbcMetadataAccessible'.
				.addService( MultiTenantConnectionProvider.class, multiTenantConnectionProvider )
				.build();

		MetadataSources ms = new MetadataSources( serviceRegistry );
		ms.addAnnotatedClass( Customer.class );
		ms.addAnnotatedClass( Invoice.class );

		Metadata metadata = ms.buildMetadata();
		final PersistentClass customerMapping = metadata.getEntityBinding( Customer.class.getName() );
		customerMapping.setCached( true );
		( (RootClass) customerMapping ).setCacheConcurrencyStrategy( "read-write" );

		HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
		tool.injectServices( serviceRegistry );

		new SchemaDropperImpl( serviceRegistry ).doDrop(
				metadata,
				serviceRegistry,
				settings,
				true,
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								acmeProvider
						)
				),
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								jbossProvider
						)
				)
		);

		new SchemaCreatorImpl( serviceRegistry ).doCreation(
				metadata,
				serviceRegistry,
				settings,
				true,
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								acmeProvider
						)
				),
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								jbossProvider
						)
				)
		);

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		configure( sfb );
		sessionFactory = (SessionFactoryImplementor) sfb.build();
	}

	protected void configure(SessionFactoryBuilder sfb) {
	}

	protected abstract T buildMultiTenantConnectionProvider();

	@After
	public void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
		if ( serviceRegistry != null ) {
			serviceRegistry.destroy();
		}
		if ( jbossProvider != null ) {
			( (Stoppable) jbossProvider ).stop();
		}
		if ( acmeProvider != null ) {
			( (Stoppable) acmeProvider ).stop();
		}
	}

	@Test
	@JiraKey(value = "HHH-16310")
	public void testJdbcMetadataAccessible() {
		assertThat( ( (ExtractedDatabaseMetaDataImpl) sessionFactory.getJdbcServices().getJdbcEnvironment()
				.getExtractedDatabaseMetaData() ).isJdbcMetadataAccessible() )
				.isTrue();
	}

	@Test
	public void testBasicExpectedBehavior() {
		Customer steve = doInHibernateSessionBuilder( this::jboss, session -> {
			Customer _steve = new Customer( 1L, "steve" );
			session.persist( _steve );
			return _steve;
		} );

		doInHibernateSessionBuilder( this::acme, session -> {
			Customer check = session.get( Customer.class, steve.getId() );
			Assert.assertNull( "tenancy not properly isolated", check );
		} );

		doInHibernateSessionBuilder( this::jboss, session -> {
			session.remove( steve );
		} );
	}

	@Test
	public void testSameIdentifiers() {
		// create a customer 'steve' in jboss
		Customer steve = doInHibernateSessionBuilder( this::jboss, session -> {
			Customer _steve = new Customer( 1L, "steve" );
			session.persist( _steve );
			return _steve;
		} );

		// now, create a customer 'john' in acme
		Customer john = doInHibernateSessionBuilder( this::acme, session -> {
			Customer _john = new Customer( 1L, "john" );
			session.persist( _john );
			return _john;
		} );

		sessionFactory.getStatistics().clear();

		// make sure we get the correct people back, from cache
		// first, jboss
		doInHibernateSessionBuilder( this::jboss, session -> {
			Customer customer = session.getReference( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 1, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		sessionFactory.getStatistics().clear();
		// then, acme
		doInHibernateSessionBuilder( this::acme, session -> {
			Customer customer = session.getReference( Customer.class, 1L );
			Assert.assertEquals( "john", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 1, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		// make sure the same works from datastore too
		sessionFactory.getStatistics().clear();
		sessionFactory.getCache().evictEntityData();
		// first jboss
		doInHibernateSessionBuilder( this::jboss, session -> {
			Customer customer = session.getReference( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		sessionFactory.getStatistics().clear();
		// then, acme
		doInHibernateSessionBuilder( this::acme, session -> {
			Customer customer = session.getReference( Customer.class, 1L );
			Assert.assertEquals( "john", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		doInHibernateSessionBuilder( this::jboss, session -> {
			session.remove( steve );
		} );

		doInHibernateSessionBuilder( this::acme, session -> {
			session.remove( john );
		} );
	}

	@Test
	public void testTableIdentifiers() {
		Invoice orderJboss = doInHibernateSessionBuilder( this::jboss, session -> {
			Invoice _orderJboss = new Invoice();
			session.persist( _orderJboss );
			Assert.assertEquals( Long.valueOf( 1 ), _orderJboss.getId() );
			return _orderJboss;
		} );

		Invoice orderAcme = doInHibernateSessionBuilder( this::acme, session -> {
			Invoice _orderAcme = new Invoice();
			session.persist( _orderAcme );
			Assert.assertEquals( Long.valueOf( 1 ), _orderAcme.getId() );
			return _orderAcme;
		} );

		doInHibernateSessionBuilder( this::jboss, session -> {
			session.remove( orderJboss );
		} );

		doInHibernateSessionBuilder( this::acme, session -> {
			session.remove( orderAcme );
		} );

		sessionFactory.getStatistics().clear();
	}

	protected SessionBuilder newSession(String tenant) {
		return sessionFactory
			.withOptions()
			.tenantIdentifier( (Object) tenant );
	}

	private SessionBuilder jboss() {
		return newSession( "jboss" );
	}

	private SessionBuilder acme() {
		return newSession( "acme" );
	}

}
