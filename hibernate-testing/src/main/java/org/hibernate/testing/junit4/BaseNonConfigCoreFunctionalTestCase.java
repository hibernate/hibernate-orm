/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.build.AllowPrintStacktrace;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jdbc.Work;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.After;
import org.junit.Before;

import static java.lang.Thread.currentThread;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.testing.util.ServiceRegistryUtil.serviceRegistryBuilder;
import static org.junit.Assert.fail;

/**
 * Applies functional testing logic for core Hibernate testing on top of {@link BaseUnitTestCase}.
 * Much like {@link BaseCoreFunctionalTestCase}, except that
 * this form uses the new bootstrapping APIs while BaseCoreFunctionalTestCase continues to
 * use (the neutered form of) Configuration.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use JUnit 5/6
 */
@Deprecated
public class BaseNonConfigCoreFunctionalTestCase extends BaseUnitTestCase {
	public static final String VALIDATE_DATA_CLEANUP = "hibernate.test.validateDataCleanup";

	private StandardServiceRegistry serviceRegistry;
	private MetadataImplementor metadata;
	private SessionFactoryImplementor sessionFactory;

	private Session session;

	protected Dialect getDialect() {
		return serviceRegistry != null
				? serviceRegistry.getService(JdbcEnvironment.class).getDialect()
				: BaseCoreFunctionalTestCase.getDialect();
	}

	protected StandardServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	protected MetadataImplementor metadata() {
		return metadata;
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

	protected Session getSession() {
		return session;
	}

	protected void rebuildSessionFactory() {
		releaseResources();
		buildResources();
	}

	protected void cleanupCache() {
		if ( sessionFactory != null ) {
			sessionFactory.getCache().evictAllRegions();
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JUNIT hooks

	@BeforeClassOnce
	protected void startUp() {
		buildResources();
	}

	protected void buildResources() {
		final StandardServiceRegistryBuilder serviceRegistryBuilder = constructStandardServiceRegistryBuilder();

		serviceRegistry = serviceRegistryBuilder.build();
		afterStandardServiceRegistryBuilt( serviceRegistry );

		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		applyMetadataSources( metadataSources );
		afterMetadataSourcesApplied( metadataSources );

		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		initialize( metadataBuilder );
		configureMetadataBuilder( metadataBuilder );
		metadata = (MetadataImplementor) metadataBuilder.build();
		if ( overrideCacheStrategy() && getCacheConcurrencyStrategy() != null ) {
			applyCacheSettings( metadata );
		}
		afterMetadataBuilt( metadata );

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		initialize( sfb, metadata );
		configureSessionFactoryBuilder( sfb );

		sessionFactory = (SessionFactoryImplementor) sfb.build();
		afterSessionFactoryBuilt( sessionFactory );
	}

	protected final StandardServiceRegistryBuilder constructStandardServiceRegistryBuilder() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		builder.applyClassLoader( getClass().getClassLoader() );
		// by default, we do not share the BootstrapServiceRegistry nor the StandardServiceRegistry,
		// so we want the BootstrapServiceRegistry to be automatically closed when the
		// StandardServiceRegistry is closed.
		builder.enableAutoClose();
		configureBootstrapServiceRegistryBuilder( builder );
		final BootstrapServiceRegistry bootstrapServiceRegistry = builder.build();
		afterBootstrapServiceRegistryBuilt( bootstrapServiceRegistry );
		final Map<String, Object> settings = defaultSettings();
		addSettings( settings );
		final StandardServiceRegistryBuilder registryBuilder =
				serviceRegistryBuilder( bootstrapServiceRegistry );
		initialize( registryBuilder );
		registryBuilder.applySettings( settings );
		configureStandardServiceRegistryBuilder( registryBuilder );
		return registryBuilder;
	}

	private static Map<String, Object> defaultSettings() {
		final Map<String,Object> settings = new HashMap<>();
		settings.put( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		settings.put( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		settings.put( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		return settings;
	}

	protected void addSettings(Map<String,Object> settings) {
	}

	/**
	 * Apply any desired config to the BootstrapServiceRegistryBuilder to be incorporated
	 * into the built BootstrapServiceRegistry
	 *
	 * @param registryBuilder The BootstrapServiceRegistryBuilder
	 */
	@SuppressWarnings("UnusedParameters")
	protected void configureBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder registryBuilder) {
	}

	/**
	 * Hook to allow tests to use the BootstrapServiceRegistry if they wish
	 *
	 * @param bootstrapServiceRegistry The BootstrapServiceRegistry
	 */
	@SuppressWarnings("UnusedParameters")
	protected void afterBootstrapServiceRegistryBuilt(BootstrapServiceRegistry bootstrapServiceRegistry) {
	}

	private void initialize(StandardServiceRegistryBuilder builder) {
		final Dialect dialect = BaseCoreFunctionalTestCase.getDialect();
		builder.applySetting( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		if ( createSchema() ) {
			builder.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
			final String secondSchemaName = createSecondSchema();
			if ( isNotEmpty( secondSchemaName ) ) {
				if ( !( dialect instanceof H2Dialect ) ) {
					// while it may be true that only H2 supports creation of a second schema via
					// URL (no idea whether that is accurate), every db should support creation of schemas
					// via DDL which SchemaExport can create for us.  See how this is used and
					// whether that usage could not just leverage that capability
					throw new UnsupportedOperationException( "Only H2 dialect supports creation of second schema." );
				}
				Helper.createH2Schema( secondSchemaName, builder.getSettings() );
			}
		}
		builder.applySetting( AvailableSettings.DIALECT, dialect.getClass().getName() );
	}

	protected boolean createSchema() {
		return true;
	}

	protected String createSecondSchema() {
		// poorly named, yes, but to keep migration easy for existing BaseCoreFunctionalTestCase
		// impls I kept the same name from there
		return null;
	}

	/**
	 * Apply any desired config to the StandardServiceRegistryBuilder to be incorporated
	 * into the built StandardServiceRegistry
	 *
	 * @param serviceRegistryBuilder The StandardServiceRegistryBuilder
	 */
	@SuppressWarnings("UnusedParameters")
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
	}

	/**
	 * Hook to allow tests to use the StandardServiceRegistry if they wish
	 *
	 * @param standardServiceRegistry The StandardServiceRegistry
	 */
	@SuppressWarnings("UnusedParameters")
	protected void afterStandardServiceRegistryBuilt(StandardServiceRegistry standardServiceRegistry) {
	}

	protected void applyMetadataSources(MetadataSources sources) {
		for ( String mapping : getMappings() ) {
			sources.addResource( getBaseForMappings() + mapping );
		}
		for ( Class<?> annotatedClass : getAnnotatedClasses() ) {
			sources.addAnnotatedClass( annotatedClass );
		}
		for ( String annotatedPackage : getAnnotatedPackages() ) {
			sources.addPackage( annotatedPackage );
		}
		for ( String ormXmlFile : getXmlFiles() ) {
			sources.addInputStream( currentThread().getContextClassLoader().getResourceAsStream( ormXmlFile ) );
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
		return NO_MAPPINGS;
	}

	protected void afterMetadataSourcesApplied(MetadataSources metadataSources) {
	}

	protected void initialize(MetadataBuilder metadataBuilder) {
		metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );
	}

	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
	}

	protected boolean overrideCacheStrategy() {
		return true;
	}

	protected String getCacheConcurrencyStrategy() {
		return null;
	}

	protected final void applyCacheSettings(Metadata metadata) {
		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( !entityBinding.isInherited() ) {
				if ( !hasLob( entityBinding ) ) {
					final RootClass rootClass = (RootClass) entityBinding;
					rootClass.setCacheConcurrencyStrategy( getCacheConcurrencyStrategy() );
					entityBinding.setCached( true );
				}
			}
		}

		for ( Collection collectionBinding : metadata.getCollectionBindings() ) {
			if ( !isLob( collectionBinding ) ) {
				collectionBinding.setCacheConcurrencyStrategy( getCacheConcurrencyStrategy() );
			}
		}
	}

	private static boolean isLob(Collection collectionBinding) {
		return collectionBinding.getElement().isSimpleValue()
				&& isLob( (SimpleValue) collectionBinding.getElement() );
	}

	private static boolean hasLob(PersistentClass entityBinding) {
		for ( Property prop : entityBinding.getPropertyClosure() ) {
			if ( prop.getValue().isSimpleValue() ) {
				if ( isLob( (SimpleValue) prop.getValue() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isLob(SimpleValue value) {
		final String typeName = value.getTypeName();
		if ( typeName != null ) {
			final String significantTypeNamePart =
					typeName.substring( typeName.lastIndexOf( '.' ) + 1 )
							.toLowerCase( Locale.ROOT );
			switch ( significantTypeNamePart ) {
				case "blob":
				case "blobtype":
				case "clob":
				case "clobtype":
				case "nclob":
				case "nclobtype":
					return true;
			}
		}
		return false;
	}

	protected void afterMetadataBuilt(Metadata metadata) {
	}

	private void initialize(SessionFactoryBuilder sessionFactoryBuilder, Metadata metadata) {
		// todo : this is where we need to apply cache settings to be like BaseCoreFunctionalTestCase
		//		it reads the class/collection mappings and creates corresponding
		//		CacheRegionDescription references.
		//
		//		Ultimately I want those to go on MetadataBuilder, and in fact MetadataBuilder
		//		already defines the needed method.  But for the [pattern used by the
		//		tests we need this as part of SessionFactoryBuilder
	}

	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
	}

	protected void afterSessionFactoryBuilt(SessionFactoryImplementor sessionFactory) {
	}

	@AfterClassOnce
	@SuppressWarnings("unused")
	protected void shutDown() {
		releaseResources();
	}

	@AllowSysOut
	@AllowPrintStacktrace
	protected void releaseResources() {
		if ( sessionFactory != null ) {
			try {
				sessionFactory.close();
			}
			catch (Exception e) {
				System.err.println( "Unable to release SessionFactory : " + e.getMessage() );
				e.printStackTrace();
			}
		}
		sessionFactory = null;

		if ( serviceRegistry != null ) {
			try {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
			catch (Exception e) {
				System.err.println( "Unable to release StandardServiceRegistry : " + e.getMessage() );
				e.printStackTrace();
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

	protected boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@Before
	public final void beforeTest() throws Exception {
		prepareTest();
	}

	protected void prepareTest() throws Exception {
	}

	@After
	public final void afterTest() throws Exception {
		try {
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
		}
		finally {
			cleanupSession();
		}
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
			try {
				session.getTransaction().rollback();
			}
			catch ( RuntimeException e ) {
				log.debug( "Ignoring exception during clean up", e );
			}
		}
	}

	private static boolean canRollback(SessionImplementor sessionImplementor) {
		return sessionImplementor.getTransactionCoordinator()
				.getTransactionDriverControl().getStatus().canRollback();
	}

	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	protected void cleanupTestData() throws Exception {
		sessionFactory.getSchemaManager().truncateMappedObjects();
//		doInHibernate( this::sessionFactory, session -> {
//			session.createSelectionQuery( "from java.lang.Object", Object.class )
//					.getResultList()
//					.forEach( session::remove );
//		} );
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

		try ( Session tmpSession = sessionFactory.openSession() ) {
			final List<Object> list =
					tmpSession.createSelectionQuery( "select o from java.lang.Object o", Object.class )
							.getResultList();
			final Map<String, Integer> items = new HashMap<>();
			if ( !list.isEmpty() ) {
				for ( Object element : list ) {
					Integer l = items.get( tmpSession.getEntityName( element ) );
					if (l == null) {
						l = 0;
					}
					l = l + 1;
					items.put( tmpSession.getEntityName( element ), l );
					System.out.println( "Data left: " + element );
				}
				fail( "Data is left in the database: " + items );
			}
		}
		// intentionally empty
	}


	public void inSession(Consumer<SessionImplementor> action) {
		log.trace( "#inSession(action)" );
		TransactionUtil2.inSession( sessionFactory(), action );
	}

	public void inStatelessSession(Consumer<StatelessSession> action) {
		log.trace( "#inSession(action)" );
		TransactionUtil2.inStatelessSession( sessionFactory(), action );
	}

	public <R> R fromSession(Function<SessionImplementor,R> action) {
		log.trace( "#inSession(action)" );
		return TransactionUtil2.fromSession( sessionFactory(), action );
	}

	public void inTransaction(Consumer<SessionImplementor> action) {
		log.trace( "#inTransaction(action)" );
		TransactionUtil2.inTransaction( sessionFactory(), action );
	}

	public void inStatelessTransaction(Consumer<StatelessSession> action) {
		log.trace( "#inTransaction(action)" );
		TransactionUtil2.inStatelessTransaction( sessionFactory(), action );
	}

	public <R> R fromTransaction(Function<SessionImplementor,R> action) {
		log.trace( "#inTransaction(action)" );
		return TransactionUtil2.fromTransaction( sessionFactory(), action );
	}
}
