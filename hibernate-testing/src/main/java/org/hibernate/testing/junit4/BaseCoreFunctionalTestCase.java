/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.SharedCacheMode;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.resource.transaction.TransactionCoordinator;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;
import org.hibernate.testing.SkipLog;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.fail;

/**
 * Applies functional testing logic for core Hibernate testing on top of {@link BaseUnitTestCase}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"deprecation"} )
public abstract class BaseCoreFunctionalTestCase extends BaseUnitTestCase {
	public static final String VALIDATE_DATA_CLEANUP = "hibernate.test.validateDataCleanup";

	public static final Dialect DIALECT = Dialect.getDialect();

	private Configuration configuration;
	private StandardServiceRegistryImpl serviceRegistry;
	private SessionFactoryImplementor sessionFactory;

	protected Session session;

	protected static Dialect getDialect() {
		return DIALECT;
	}

	protected Configuration configuration() {
		return configuration;
	}

	protected StandardServiceRegistryImpl serviceRegistry() {
		return serviceRegistry;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected Session openSession() throws HibernateException {
		session = sessionFactory().openSession();
		return session;
	}

	protected Session openSession(Interceptor interceptor) throws HibernateException {
		session = sessionFactory().withOptions().interceptor( interceptor ).openSession();
		return session;
	}


	// before/after test class ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@BeforeClassOnce
	@SuppressWarnings( {"UnusedDeclaration"})
	protected void buildSessionFactory() {
		// for now, build the configuration to get all the property settings
		configuration = constructAndConfigureConfiguration();
		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		serviceRegistry = buildServiceRegistry( bootRegistry, configuration );
		// this is done here because Configuration does not currently support 4.0 xsd
		afterConstructAndConfigureConfiguration( configuration );
		sessionFactory = ( SessionFactoryImplementor ) configuration.buildSessionFactory( serviceRegistry );
		afterSessionFactoryBuilt();
	}

	protected void rebuildSessionFactory() {
		if ( sessionFactory == null ) {
			return;
		}
		try {
			sessionFactory.close();
			sessionFactory = null;
			configuration = null;
			serviceRegistry.destroy();
			serviceRegistry = null;
		}
		catch (Exception ignore) {
		}

		buildSessionFactory();
	}

	protected Configuration buildConfiguration() {
		Configuration cfg = constructAndConfigureConfiguration();
		afterConstructAndConfigureConfiguration( cfg );
		return cfg;
	}

	protected Configuration constructAndConfigureConfiguration() {
		Configuration cfg = constructConfiguration();
		configure( cfg );
		return cfg;
	}

	private void afterConstructAndConfigureConfiguration(Configuration cfg) {
		addMappings( cfg );
		applyCacheSettings( cfg );
		afterConfigurationBuilt( cfg );
	}

	protected Configuration constructConfiguration() {
		Configuration configuration = new Configuration();
		configuration.setProperty( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		configuration.setProperty( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		if ( createSchema() ) {
			configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			final String secondSchemaName = createSecondSchema();
			if ( StringHelper.isNotEmpty( secondSchemaName ) ) {
				if ( !( getDialect() instanceof H2Dialect ) ) {
					throw new UnsupportedOperationException( "Only H2 dialect supports creation of second schema." );
				}
				Helper.createH2Schema( secondSchemaName, configuration );
			}
		}
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );
		configuration.setProperty( Environment.DIALECT, getDialect().getClass().getName() );
		return configuration;
	}

	protected void configure(Configuration configuration) {
	}

	protected void addMappings(Configuration configuration) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				configuration.addResource(
						getBaseForMappings() + mapping,
						getClass().getClassLoader()
				);
			}
		}
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				configuration.addAnnotatedClass( annotatedClass );
			}
		}
		String[] annotatedPackages = getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				configuration.addPackage( annotatedPackage );
			}
		}
		String[] xmlFiles = getXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				configuration.addInputStream( is );
			}
		}
	}

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getAnnotatedPackages() {
		return NO_MAPPINGS;
	}

	protected String[] getXmlFiles() {
		// todo : rename to getOrmXmlFiles()
		return NO_MAPPINGS;
	}

	protected void applyCacheSettings(Configuration configuration) {
		if ( getCacheConcurrencyStrategy() != null ) {
			configuration.setProperty( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, getCacheConcurrencyStrategy() );
			configuration.setSharedCacheMode( SharedCacheMode.ALL );
		}
	}

	protected String getCacheConcurrencyStrategy() {
		return null;
	}

	protected void afterConfigurationBuilt(Configuration configuration) {
	}

	protected BootstrapServiceRegistry buildBootstrapServiceRegistry() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		prepareBootstrapRegistryBuilder( builder );
		return builder.build();
	}

	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
	}

	protected StandardServiceRegistryImpl buildServiceRegistry(BootstrapServiceRegistry bootRegistry, Configuration configuration) {
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );

		StandardServiceRegistryBuilder cfgRegistryBuilder = configuration.getStandardServiceRegistryBuilder();

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( bootRegistry, cfgRegistryBuilder.getAggregatedCfgXml() )
				.applySettings( properties );

		prepareBasicRegistryBuilder( registryBuilder );
		return (StandardServiceRegistryImpl) registryBuilder.build();
	}

	protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
	}

	protected void afterSessionFactoryBuilt() {
	}

	protected boolean createSchema() {
		return true;
	}

	/**
	 * Feature supported only by H2 dialect.
	 * @return Provide not empty name to create second schema.
	 */
	protected String createSecondSchema() {
		return null;
	}

	protected boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@AfterClassOnce
	@SuppressWarnings( {"UnusedDeclaration"})
	protected void releaseSessionFactory() {
		if ( sessionFactory == null ) {
			return;
		}
		sessionFactory.close();
		sessionFactory = null;
		configuration = null;
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

	@OnFailure
	@OnExpectedFailure
	@SuppressWarnings( {"UnusedDeclaration"})
	public void onFailure() {
		if ( rebuildSessionFactoryOnError() ) {
			rebuildSessionFactory();
		}
	}


	// before/after each test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Before
	public final void beforeTest() throws Exception {
		prepareTest();
	}

	protected void prepareTest() throws Exception {
	}

	@After
	public final void afterTest() throws Exception {
		completeStrayTransaction();

		if ( isCleanupTestDataRequired() ) {
			cleanupTestData();
		}
		cleanupTest();

		cleanupSession();

		assertAllDataRemoved();

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
	}

	protected void cleanupCache() {
		if ( sessionFactory != null ) {
			sessionFactory.getCache().evictAllRegions();
		}
	}
	
	protected boolean isCleanupTestDataRequired() {
		return false;
	}
	
	protected void cleanupTestData() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "delete from java.lang.Object" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}


	private void cleanupSession() {
		if ( session != null && ! ( (SessionImplementor) session ).isClosed() ) {
			session.close();
		}
		session = null;
	}

	public class RollbackWork implements Work {
		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}

	protected void cleanupTest() throws Exception {
	}

	@SuppressWarnings( {"UnnecessaryBoxing", "UnnecessaryUnboxing"})
	protected void assertAllDataRemoved() {
		if ( !createSchema() ) {
			return; // no tables were created...
		}
		if ( !Boolean.getBoolean( VALIDATE_DATA_CLEANUP ) ) {
			return;
		}

		Session tmpSession = sessionFactory.openSession();
		try {
			List list = tmpSession.createQuery( "select o from java.lang.Object o" ).list();

			Map<String,Integer> items = new HashMap<String,Integer>();
			if ( !list.isEmpty() ) {
				for ( Object element : list ) {
					Integer l = items.get( tmpSession.getEntityName( element ) );
					if ( l == null ) {
						l = 0;
					}
					l = l + 1 ;
					items.put( tmpSession.getEntityName( element ), l );
					System.out.println( "Data left: " + element );
				}
				fail( "Data is left in the database: " + items.toString() );
			}
		}
		finally {
			try {
				tmpSession.close();
			}
			catch( Throwable t ) {
				// intentionally empty
			}
		}
	}

	protected boolean readCommittedIsolationMaintained(String scenario) {
		int isolation = java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
		Session testSession = null;
		try {
			testSession = openSession();
			isolation = testSession.doReturningWork(
					new AbstractReturningWork<Integer>() {
						@Override
						public Integer execute(Connection connection) throws SQLException {
							return connection.getTransactionIsolation();
						}
					}
			);
		}
		catch( Throwable ignore ) {
		}
		finally {
			if ( testSession != null ) {
				try {
					testSession.close();
				}
				catch( Throwable ignore ) {
				}
			}
		}
		if ( isolation < java.sql.Connection.TRANSACTION_READ_COMMITTED ) {
			SkipLog.reportSkip( "environment does not support at least read committed isolation", scenario );
			return false;
		}
		else {
			return true;
		}
	}
}
