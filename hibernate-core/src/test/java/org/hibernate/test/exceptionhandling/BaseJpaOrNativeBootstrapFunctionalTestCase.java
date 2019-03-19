/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.exceptionhandling;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;

import static org.junit.Assert.fail;

/**
 * A base class for all functional tests.
 */
public abstract class BaseJpaOrNativeBootstrapFunctionalTestCase extends BaseUnitTestCase {

	// IMPL NOTE : Here we use @Before and @After (instead of @BeforeClassOnce and @AfterClassOnce like we do in
	// BaseCoreFunctionalTestCase) because the old HEM test methodology was to create an EMF for each test method.

	private static final Dialect dialect = Dialect.getDialect();

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
	@SuppressWarnings( {"UnusedDeclaration"})
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

		Properties properties = buildProperties();
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( getAnnotatedClasses() ) );
		properties.put( AvailableSettings.LOADED_CLASSES, classes );

		sessionFactory =  Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				properties
		).build().unwrap( SessionFactoryImplementor.class );

		serviceRegistry = (StandardServiceRegistryImpl) sessionFactory.getServiceRegistry()
				.getParentServiceRegistry();
	}

	private void buildSessionFactory() {
		// for now, build the configuration to get all the property settings
		Configuration configuration = new Configuration();
		configuration.setProperties( buildProperties() );

		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				configuration.addAnnotatedClass( annotatedClass );
			}
		}

		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		serviceRegistry = buildServiceRegistry( bootRegistry, configuration );
		sessionFactory = ( SessionFactoryImplementor ) configuration.buildSessionFactory( serviceRegistry );

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
		public PersistenceUnitTransactionType getTransactionType() {
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
	}

	private BootstrapServiceRegistry buildBootstrapServiceRegistry() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		builder.applyClassLoader( getClass().getClassLoader() );
		prepareBootstrapRegistryBuilder( builder );
		return builder.build();
	}

	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
	}

	private StandardServiceRegistryImpl buildServiceRegistry(BootstrapServiceRegistry bootRegistry, Configuration configuration) {
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		ConfigurationHelper.resolvePlaceHolders( properties );

		StandardServiceRegistryBuilder cfgRegistryBuilder = configuration.getStandardServiceRegistryBuilder();

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( bootRegistry, cfgRegistryBuilder.getAggregatedCfgXml() )
				.applySettings( properties );

		return (StandardServiceRegistryImpl) registryBuilder.build();
	}

	private Properties buildProperties() {
		Properties properties = Environment.getProperties();

		properties.put( org.hibernate.cfg.AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			properties.put( AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(), entry.getValue() );
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			properties.put( AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(), entry.getValue() );
		}

		configure( properties );

		if ( createSchema() ) {
			properties.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}
		properties.put( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		properties.put( org.hibernate.cfg.AvailableSettings.DIALECT, getDialect().getClass().getName() );

		return properties;
	}

	protected void configure(Map<Object, Object> properties) {
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	public Map<Class, String> getCachedClasses() {
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
	public final void afterTest() throws Exception {
		completeStrayTransaction();

		cleanupSession();

	}

	@AfterClassOnce
	@SuppressWarnings( {"UnusedDeclaration"})
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

		if ( ( (SessionImplementor) session ).isClosed() ) {
			// nothing to do
			return;
		}

		if ( !session.isConnected() ) {
			// nothing to do
			return;
		}

		final TransactionCoordinator.TransactionDriver tdc =
				( (SessionImplementor) session ).getTransactionCoordinator().getTransactionDriverControl();

		if ( tdc.getStatus().canRollback() ) {
			session.getTransaction().rollback();
		}
		session.close();
	}

	private void cleanupSession() {
		if ( session != null && ! ( (SessionImplementor) session ).isClosed() ) {
			session.close();
		}
		session = null;
	}
}
