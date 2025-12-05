/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hibernate.internal.util.config.ConfigurationHelper.resolvePlaceHolders;
import static org.hibernate.jpa.boot.spi.Bootstrap.getEntityManagerFactoryBuilder;

/**
 * A base class for all functional tests.
 */
@BaseUnitTest
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
public abstract class BaseJpaOrNativeBootstrapFunctionalTestCase {
	protected static final Logger log = Logger.getLogger( BaseJpaOrNativeBootstrapFunctionalTestCase.class );
	protected static final Dialect dialect = DialectContext.getDialect();

	public enum BootstrapMethod { JPA, NATIVE }

	private final BootstrapMethod bootstrapMethod;

	private ServiceRegistryImplementor serviceRegistry;
	private SessionFactoryImplementor sessionFactory;

	protected Dialect getDialect() {
		return dialect;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected ServiceRegistryImplementor serviceRegistry() {
		return serviceRegistry;
	}

	protected BaseJpaOrNativeBootstrapFunctionalTestCase(BootstrapMethod bootstrapMethod) {
		this.bootstrapMethod = bootstrapMethod;
	}

	@AfterEach
	void cleanUpState() {
		if ( sessionFactory != null ) {
			sessionFactory.getSchemaManager().truncateMappedObjects();
			sessionFactory.close();
			sessionFactory = null;
		}

		if ( serviceRegistry != null ) {
			if ( serviceRegistry.isActive() ) {
				serviceRegistry.destroy();
			}
			serviceRegistry = null;
		}
	}

	@BeforeEach
	void prepareState() {
		switch ( bootstrapMethod ) {
			case JPA:
				buildEntityManagerFactory();
				break;
			case NATIVE:
				buildSessionFactory();
				break;
		}
	}

	private void buildEntityManagerFactory() {
		log.trace( "Building EntityManagerFactory" );

		final Properties properties = buildProperties();
		properties.put( AvailableSettings.LOADED_CLASSES, List.of( getAnnotatedClasses() ) );
		ServiceRegistryUtil.applySettings( properties );

		sessionFactory = getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), properties )
				.build()
				.unwrap( SessionFactoryImplementor.class );

		serviceRegistry = (StandardServiceRegistryImpl) sessionFactory.getServiceRegistry().getParentServiceRegistry();
	}

	private void buildSessionFactory() {
		// for now, build the configuration to get all the property settings
		final Configuration configuration = new Configuration();
		configuration.setProperties( buildProperties() );

		final Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				configuration.addAnnotatedClass( annotatedClass );
			}
		}

		serviceRegistry = buildServiceRegistry( buildBootstrapServiceRegistry(), configuration );
		sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( serviceRegistry );
	}

	private ServiceRegistryImplementor buildServiceRegistry(
			BootstrapServiceRegistry bootRegistry,
			Configuration configuration) {
		var properties = new Properties();
		properties.putAll( configuration.getProperties() );
		resolvePlaceHolders( properties );
		var loadedConfig = configuration.getStandardServiceRegistryBuilder().getAggregatedCfgXml();
		var registryBuilder = new StandardServiceRegistryBuilder( bootRegistry, loadedConfig )
						.applySettings( properties );
		ServiceRegistryUtil.applySettings( registryBuilder );
		return (ServiceRegistryImplementor) registryBuilder.build();
	}

	private BootstrapServiceRegistry buildBootstrapServiceRegistry() {
		return new BootstrapServiceRegistryBuilder()
				.applyClassLoader( getClass().getClassLoader() )
				.build();
	}

	private Properties buildProperties() {
		final Properties properties = Environment.getProperties();
		properties.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		configure( PropertiesHelper.map( properties ) );
		properties.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		properties.put( AvailableSettings.DIALECT, getDialect().getClass().getName() );
		return properties;
	}

	protected void configure(Map<String, Object> properties) {
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}
}
