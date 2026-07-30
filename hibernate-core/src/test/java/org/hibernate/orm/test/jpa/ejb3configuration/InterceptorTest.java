/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.orm.test.jpa.SettingsGenerator;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
public class InterceptorTest {

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Distributor.class
		};
	}

	private EntityManagerFactory entityManagerFactory;

	@AfterEach
	public void releaseResources() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
	}

	public EntityManagerFactory entityManagerFactory() {
		return entityManagerFactory;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// test deprecated Interceptor settings

	@Test
	public void testDeprecatedConfiguredInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
	}

	@Test
	public void testDeprecatedConfiguredSessionInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.SESSION_SCOPED_INTERCEPTOR, LocalExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// test Interceptor settings

	@Test
	public void testConfiguredInterceptor() {
		Map settings = basicSettings();
		settings.put( org.hibernate.cfg.AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
	}

	@Test
	public void testConfiguredSessionInterceptor() {
		Map settings = basicSettings();
		settings.put( org.hibernate.cfg.AvailableSettings.SESSION_SCOPED_INTERCEPTOR, LocalExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
	}

	@Test
	public void testConfiguredSessionInterceptorWithSessionFactory() {

		StandardServiceRegistryImpl standardRegistry = ServiceRegistryUtil.serviceRegistry();

		SessionFactory sessionFactory = null;

		try {
			Metadata metadata = MetadataBuildingTestHelper.buildMetadata(
					standardRegistry,
					getAnnotatedClasses()
			);

			final SessionFactoryOptionsCollector optionsCollector = new SessionFactoryOptionsCollector();
			optionsCollector.applyStatelessInterceptorSupplier( () -> new LocalExceptionInterceptor() );
			sessionFactory = SessionFactoryPipeline.build( metadata, optionsCollector );

			final SessionFactory sessionFactoryInstance = sessionFactory;

			Supplier<SessionFactory> sessionFactorySupplier = () -> sessionFactoryInstance;

			Item i = new Item();
			i.setName( "Laptop" );

			try {
				doInHibernate( sessionFactorySupplier, session -> {
					session.persist( i );
					fail( "No interceptor" );
					return null;
				});
			}
			catch ( IllegalStateException e ) {
				assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
			}
		}
		finally {
			if(sessionFactory != null) {
				sessionFactory.close();
			}
			standardRegistry.destroy();
		}
	}

	@Test
	public void testConfiguredSessionInterceptorSupplier() {
		Map settings = basicSettings();
		settings.put( org.hibernate.cfg.AvailableSettings.SESSION_SCOPED_INTERCEPTOR, (Supplier<Interceptor>) LocalExceptionInterceptor::new);
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
	}

	@Test
	public void testEmptyCreateEntityManagerFactoryAndPropertyUse() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
	}

	@Test
	public void testOnLoadCallInInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, new ExceptionInterceptor( true ) );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );

				entityManager.persist( i );
				entityManager.flush();
				entityManager.clear();
				try {
					entityManager.find( Item.class, i.getName() );
					fail( "No interceptor" );
				}
				catch ( IllegalStateException e ) {
					assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
				}
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
	}


	protected Map<String, Object> basicSettings() {
		return SettingsGenerator.generateSettings(
				AvailableSettings.HBM2DDL_AUTO, "create-drop",
				AvailableSettings.DIALECT, DialectContext.getDialect().getClass().getName()
		);
	}

	private void buildEntityManagerFactory(Map<String,Object> settings) {
		entityManagerFactory = org.hibernate.boot.pipeline.internal.BootstrapPipeline
				.build(
						new PersistenceUnitDescriptorAdapter() {
							@Override
							public List<String> getManagedClassNames() {
								return List.of(
										Distributor.class.getName(),
										Item.class.getName()
								);
							}
						},
						settings
				);
	}

}
