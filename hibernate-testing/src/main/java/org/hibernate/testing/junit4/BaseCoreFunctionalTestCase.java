/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.persistence.SharedCacheMode;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
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
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;
import org.hibernate.testing.SkipLog;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;

import static java.lang.Thread.currentThread;
import static org.hibernate.internal.util.config.ConfigurationHelper.resolvePlaceHolders;
import static org.junit.Assert.fail;

/**
 * Applies functional testing logic for core Hibernate testing on top of {@link BaseUnitTestCase}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("deprecation")
public abstract class BaseCoreFunctionalTestCase extends BaseUnitTestCase {
	public static final String VALIDATE_DATA_CLEANUP = "hibernate.test.validateDataCleanup";

	public static final Dialect DIALECT = DialectContext.getDialect();

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
	protected void buildSessionFactory() {
		buildSessionFactory( null );
	}

	protected void buildSessionFactory(Consumer<Configuration> configurationAdapter) {
		// for now, build the configuration to get all the property settings
		BootstrapServiceRegistry bootRegistry = null;
		try {
			bootRegistry = buildBootstrapServiceRegistry();
			configuration = constructAndConfigureConfiguration( bootRegistry );
			if ( configurationAdapter != null ) {
				configurationAdapter.accept( configuration );
			}
			serviceRegistry = buildServiceRegistry( bootRegistry, configuration );
			// this is done here because Configuration does not currently support 4.0 xsd
			afterConstructAndConfigureConfiguration( configuration );
			sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( serviceRegistry );
			afterSessionFactoryBuilt();
		}
		catch (Throwable t) {
			if ( sessionFactory != null ) {
				sessionFactory.close();
				sessionFactory = null;
				configuration = null;
			}
			if ( serviceRegistry != null ) {
				serviceRegistry.destroy();
				serviceRegistry = null;
			}
			else if ( bootRegistry != null ) {
				bootRegistry.close();
			}
			throw t;
		}
	}

	protected void rebuildSessionFactory() {
		rebuildSessionFactory( null );
	}

	protected void rebuildSessionFactory(Consumer<Configuration> configurationAdapter) {
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

		buildSessionFactory( configurationAdapter );
	}

	protected Configuration constructAndConfigureConfiguration(BootstrapServiceRegistry bootstrapServiceRegistry) {
		Configuration cfg = constructConfiguration( bootstrapServiceRegistry );
		configure( cfg );
		return cfg;
	}

	private void afterConstructAndConfigureConfiguration(Configuration cfg) {
		addMappings( cfg );
		applyCacheSettings( cfg );
		afterConfigurationBuilt( cfg );
	}

	protected Configuration constructConfiguration(BootstrapServiceRegistry bootstrapServiceRegistry) {
		final Configuration configuration = new Configuration( bootstrapServiceRegistry );
		configuration.setProperty( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
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
		configuration.setProperty( Environment.DIALECT, getDialect().getClass() );
		configuration.getProperties().put( PersistentTableStrategy.DROP_ID_TABLES, true );
		configuration.getProperties().put( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, true );
		configuration.getProperties().put( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, true );
		ServiceRegistryUtil.applySettings( configuration.getStandardServiceRegistryBuilder() );
		return configuration;
	}

	protected void configure(Configuration configuration) {
	}

	protected void addMappings(Configuration configuration) {
		final String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				if ( mapping.startsWith( "/" ) ) {
					configuration.addResource( mapping );
				}
				else {
					configuration.addResource( getBaseForMappings() + mapping );
				}
			}
		}
		final Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				configuration.addAnnotatedClass( annotatedClass );
			}
		}
		final String[] annotatedPackages = getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				configuration.addPackage( annotatedPackage );
			}
		}
		final String[] xmlFiles = getOrmXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				try ( InputStream is = currentThread().getContextClassLoader().getResourceAsStream( xmlFile ) ) {
					configuration.addInputStream( is );
				}
				catch (IOException e) {
					throw new IllegalArgumentException( e );
				}
			}
		}
	}

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getAnnotatedPackages() {
		return NO_MAPPINGS;
	}

	protected String[] getOrmXmlFiles() {
		return NO_MAPPINGS;
	}

	protected void applyCacheSettings(Configuration configuration) {
		if ( getCacheConcurrencyStrategy() != null ) {
			configuration.setProperty( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					getCacheConcurrencyStrategy() );
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
		builder.applyClassLoader( getClass().getClassLoader() );
		prepareBootstrapRegistryBuilder( builder );
		return builder.build();
	}

	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
	}

	protected StandardServiceRegistryImpl buildServiceRegistry(
			BootstrapServiceRegistry bootRegistry, Configuration configuration) {
		try {
			final Properties properties = new Properties();
			properties.putAll( configuration.getProperties() );
			resolvePlaceHolders( properties );
			final LoadedConfig loadedConfig =
					configuration.getStandardServiceRegistryBuilder().getAggregatedCfgXml();
			final StandardServiceRegistryBuilder registryBuilder =
					new StandardServiceRegistryBuilder( bootRegistry, loadedConfig )
							.applySettings( properties );
			ServiceRegistryUtil.applySettings( registryBuilder );
			prepareBasicRegistryBuilder( registryBuilder );
			return (StandardServiceRegistryImpl) registryBuilder.build();
		}
		catch (Throwable t) {
			if ( bootRegistry != null ) {
				bootRegistry.close();
			}
			throw t;
		}
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
	@SuppressWarnings("unused")
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
	@SuppressWarnings("unused")
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
		// see https://github.com/hibernate/hibernate-orm/pull/3412#issuecomment-678338398
		if ( getDialect() instanceof H2Dialect ) {
			ReflectHelper.getMethod( Class.forName( "org.h2.util.DateTimeUtils" ), "resetCalendar" )
					.invoke( null );
		}
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

		final SessionImplementor sessionImplementor = (SessionImplementor) session;

		if ( sessionImplementor.isClosed() ) {
			// nothing to do
			return;
		}

		if ( !session.isConnected() ) {
			// nothing to do
			return;
		}

		if ( canRollback( sessionImplementor ) ) {
			session.getTransaction().rollback();
		}
		session.close();
	}

	private static boolean canRollback(SessionImplementor sessionImplementor) {
		return sessionImplementor.getTransactionCoordinator()
				.getTransactionDriverControl().getStatus().canRollback();
	}

	protected void cleanupCache() {
		if ( sessionFactory != null ) {
			sessionFactory.getCache().evictAllRegions();
		}
	}

	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	protected boolean isCleanupTestDataUsingBulkDelete() {
		return false;
	}

	protected void cleanupTestData() throws Exception {
		sessionFactory.getSchemaManager().truncateMappedObjects();
//		if ( isCleanupTestDataUsingBulkDelete() ) {
//			doInHibernate( this::sessionFactory, session -> {
//				session.createMutationQuery( "delete from java.lang.Object" )
//						.executeUpdate();
//			} );
//		}
//		else {
//			// Because of https://hibernate.atlassian.net/browse/HHH-5529,
//			// we can't rely on a Bulk Delete query which will not clear the link tables in @ElementCollection or unidirectional collections
//			doInHibernate( this::sessionFactory, session -> {
//				session.createSelectionQuery( "from java.lang.Object", Object.class )
//						.getResultList()
//						.forEach( session::remove );
//			} );
//		}
	}

	private void cleanupSession() {
		if ( session != null && ! ( (SessionImplementor) session ).isClosed() ) {
			session.close();
		}
		session = null;
	}

	public static class RollbackWork implements Work {
		@Override
		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}

	protected void cleanupTest() throws Exception {
	}

	@AllowSysOut
	protected void assertAllDataRemoved() {
		if ( !createSchema() ) {
			return; // no tables were created...
		}
		if ( !Boolean.getBoolean( VALIDATE_DATA_CLEANUP ) ) {
			return;
		}

		final Session tmpSession = sessionFactory.openSession();
		final Transaction transaction = tmpSession.beginTransaction();
		try {
			final List<?> list =
					tmpSession.createSelectionQuery( "select o from java.lang.Object o", Object.class )
							.getResultList();
			final Map<String,Integer> items = new HashMap<>();
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
				transaction.rollback();
				fail( "Data is left in the database: " + items );
			}
			transaction.rollback();
		}
		finally {
			try {
				if ( transaction.getStatus().canRollback() ) {
					transaction.rollback();
				}
				tmpSession.close();
			}
			catch ( Throwable t ) {
				// intentionally empty
			}
		}
	}

	protected boolean readCommittedIsolationMaintained(String scenario) {
		int isolation = Connection.TRANSACTION_READ_UNCOMMITTED;
		try ( Session testSession = openSession() ) {
			isolation = testSession.doReturningWork(
					new AbstractReturningWork<>() {
						@Override
						public Integer execute(Connection connection) throws SQLException {
							return connection.getTransactionIsolation();
						}
					}
			);
		}
		catch (Throwable ignore) {
		}
		if ( isolation < Connection.TRANSACTION_READ_COMMITTED ) {
			SkipLog.reportSkip( "environment does not support at least read committed isolation", scenario );
			return false;
		}
		else {
			return true;
		}
	}

	protected void inTransaction(Consumer<SessionImplementor> action) {
		TransactionUtil2.inTransaction( sessionFactory(), action );
	}

	protected <T> T fromTransaction(Function<SessionImplementor,T> action) {
		return TransactionUtil2.fromTransaction( sessionFactory(), action );
	}

	protected void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
		TransactionUtil2.inTransaction( session, action );
	}

	protected void inSession(Consumer<SessionImplementor> action) {
		TransactionUtil2.inSession( sessionFactory(), action );
	}

	protected void inStatelessSession(Consumer<StatelessSession> action) {
		TransactionUtil2.inStatelessSession( sessionFactory(), action );
	}
}
