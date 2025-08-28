/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;

import jakarta.persistence.EntityManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.PersistenceUnitTransactionType;

import static org.hibernate.internal.util.config.ConfigurationHelper.resolvePlaceHolders;
import static org.hibernate.jpa.boot.spi.Bootstrap.getEntityManagerFactoryBuilder;
import static org.junit.Assert.fail;

/**
 * A base class for all functional tests.
 */
public abstract class BaseJpaOrNativeBootstrapFunctionalTestCase extends BaseUnitTestCase {

	// IMPL NOTE : Here we use @Before and @After (instead of @BeforeClassOnce and @AfterClassOnce like we do in
	// BaseCoreFunctionalTestCase) because the old HEM test methodology was to create an EMF for each test method.

	private static final Dialect dialect = DialectContext.getDialect();

	public enum BootstrapMethod {
		JPA,
		NATIVE
	}

	private final BootstrapMethod bootstrapMethod;

	private StandardServiceRegistryImpl serviceRegistry;
	private SessionFactoryImplementor sessionFactory;

	private Session session;

	protected Dialect getDialect() {
		return dialect;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected StandardServiceRegistryImpl serviceRegistry() {
		return serviceRegistry;
	}

	protected Session openSession() throws HibernateException {
		session = sessionFactory().openSession();
		return session;
	}

	protected Session openSession(Interceptor interceptor) throws HibernateException {
		session = sessionFactory().withOptions().interceptor( interceptor ).openSession();
		return session;
	}

	protected EntityManager openEntityManager() throws HibernateException {
		return openSession().unwrap( EntityManager.class );
	}

	protected BaseJpaOrNativeBootstrapFunctionalTestCase(BootstrapMethod bootstrapMethod) {
		this.bootstrapMethod = bootstrapMethod;
	}

	@BeforeClassOnce
	public void buildSessionOrEntityManagerFactory() {
		switch ( bootstrapMethod ) {
			case JPA:
				buildEntityManagerFactory();
				break;
			case NATIVE:
				buildSessionFactory();
				break;
		}
		afterSessionOrEntityManagerFactoryBuilt();
	}

	private void buildEntityManagerFactory() {
		log.trace( "Building EntityManagerFactory" );

		final Properties properties = buildProperties();
		properties.put( AvailableSettings.LOADED_CLASSES, List.of( getAnnotatedClasses() ) );
		ServiceRegistryUtil.applySettings( properties );

		sessionFactory =
				getEntityManagerFactoryBuilder( buildPersistenceUnitDescriptor(), properties )
						.build().unwrap( SessionFactoryImplementor.class );

		serviceRegistry = (StandardServiceRegistryImpl)
				sessionFactory.getServiceRegistry().getParentServiceRegistry();
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

		afterSessionOrEntityManagerFactoryBuilt();
	}


	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	public static class TestingPersistenceUnitDescriptorImpl implements PersistenceUnitDescriptor {
		private final String name;

		public TestingPersistenceUnitDescriptorImpl(String name) {
			this.name = name;
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getProviderClassName() {
			return HibernatePersistenceProvider.class.getName();
		}

		@Override
		public boolean isUseQuotedIdentifiers() {
			return false;
		}

		@Override
		public boolean isExcludeUnlistedClasses() {
			return false;
		}

		@Override
		public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
			return null;
		}

		@Override @SuppressWarnings("removal")
		public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
			return null;
		}

		@Override
		public ValidationMode getValidationMode() {
			return null;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return null;
		}

		@Override
		public List<String> getManagedClassNames() {
			return null;
		}

		@Override
		public List<String> getMappingFileNames() {
			return null;
		}

		@Override
		public List<URL> getJarFileUrls() {
			return null;
		}

		@Override
		public Object getNonJtaDataSource() {
			return null;
		}

		@Override
		public Object getJtaDataSource() {
			return null;
		}

		@Override
		public Properties getProperties() {
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			return null;
		}

		@Override
		public ClassLoader getTempClassLoader() {
			return null;
		}

		@Override
		public void pushClassTransformer(EnhancementContext enhancementContext) {
		}

		@Override
		public ClassTransformer getClassTransformer() {
			return null;
		}
	}

	private BootstrapServiceRegistry buildBootstrapServiceRegistry() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		builder.applyClassLoader( getClass().getClassLoader() );
		prepareBootstrapRegistryBuilder( builder );
		return builder.build();
	}

	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
	}

	private StandardServiceRegistryImpl buildServiceRegistry(
			BootstrapServiceRegistry bootRegistry, Configuration configuration) {
		final Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		resolvePlaceHolders( properties );
		final LoadedConfig loadedConfig =
				configuration.getStandardServiceRegistryBuilder().getAggregatedCfgXml();
		final StandardServiceRegistryBuilder registryBuilder =
				new StandardServiceRegistryBuilder( bootRegistry, loadedConfig )
						.applySettings( properties );
		ServiceRegistryUtil.applySettings( registryBuilder );
		return (StandardServiceRegistryImpl) registryBuilder.build();
	}

	private Properties buildProperties() {
		final Properties properties = Environment.getProperties();
		properties.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		for ( Map.Entry<Class<?>, String> entry : getCachedClasses().entrySet() ) {
			properties.put( AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(), entry.getValue() );
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			properties.put( AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(), entry.getValue() );
		}

		configure( PropertiesHelper.map( properties ) );

		if ( createSchema() ) {
			properties.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}
		properties.put( AvailableSettings.DIALECT, getDialect().getClass().getName() );

		return properties;
	}

	protected void configure(Map<String, Object> properties) {
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	public Map<Class<?>, String> getCachedClasses() {
		return new HashMap<>();
	}

	public Map<String, String> getCachedCollections() {
		return new HashMap<>();
	}

	protected void afterSessionOrEntityManagerFactoryBuilt() {
	}

	protected boolean createSchema() {
		return true;
	}

	@After
	public final void afterTest()  {
		completeStrayTransaction();
		cleanupSession();
	}

	@AfterClassOnce
	@SuppressWarnings("unused")
	protected void releaseSessionFactory() {
		if ( sessionFactory == null ) {
			return;
		}
		sessionFactory.close();
		sessionFactory = null;
		if ( serviceRegistry != null ) {
			if ( serviceRegistry.isActive() ) {
				try {
					serviceRegistry.destroy();
				}
				catch (Exception ignore) {
				}
				fail( "StandardServiceRegistry was not closed down as expected" );
			}
		}
		serviceRegistry=null;
	}

	private void completeStrayTransaction() {
		if ( session == null ) {
			// nothing to do
			return;
		}

		final SessionImplementor sessionImplementor = (SessionImplementor) session;

		if ( sessionImplementor.isClosed() ) {
			// nothing to do
			return;
		}

		if ( !session.isConnected() ) {
			// nothing to do
			return;
		}

		if ( canRollBack( sessionImplementor ) ) {
			session.getTransaction().rollback();
		}
		session.close();
	}

	private static boolean canRollBack(SessionImplementor sessionImplementor) {
		return sessionImplementor.getTransactionCoordinator()
				.getTransactionDriverControl().getStatus().canRollback();
	}

	private void cleanupSession() {
		if ( session != null && ! ( (SessionImplementor) session ).isClosed() ) {
			session.close();
		}
		session = null;
	}
}
