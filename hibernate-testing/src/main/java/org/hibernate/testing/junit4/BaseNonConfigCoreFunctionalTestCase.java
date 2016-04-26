/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.Work;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.type.BlobType;
import org.hibernate.type.ClobType;
import org.hibernate.type.NClobType;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.fail;

/**
 * Applies functional testing logic for core Hibernate testing on top of {@link BaseUnitTestCase}.
 * Much like {@link org.hibernate.testing.junit4.BaseCoreFunctionalTestCase}, except that
 * this form uses the new bootstrapping APIs while BaseCoreFunctionalTestCase continues to
 * use (the neutered form of) Configuration.
 *
 * @author Steve Ebersole
 */
public class BaseNonConfigCoreFunctionalTestCase extends BaseUnitTestCase {
	public static final String VALIDATE_DATA_CLEANUP = "hibernate.test.validateDataCleanup";

	private StandardServiceRegistry serviceRegistry;
	private MetadataImplementor metadata;
	private SessionFactoryImplementor sessionFactory;

	private Session session;

	protected Dialect getDialect() {
		if ( serviceRegistry != null ) {
			return serviceRegistry.getService( JdbcEnvironment.class ).getDialect();
		}
		else {
			return BaseCoreFunctionalTestCase.getDialect();
		}
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
	@SuppressWarnings( {"UnusedDeclaration"})
	protected void startUp() {
		buildResources();
	}

	protected void buildResources() {
		final StandardServiceRegistryBuilder ssrb = constructStandardServiceRegistryBuilder();

		serviceRegistry = ssrb.build();
		afterStandardServiceRegistryBuilt( serviceRegistry );

		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		applyMetadataSources( metadataSources );
		afterMetadataSourcesApplied( metadataSources );

		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		initialize( metadataBuilder );
		configureMetadataBuilder( metadataBuilder );

		metadata = (MetadataImplementor) metadataBuilder.build();
		applyCacheSettings( metadata );
		afterMetadataBuilt( metadata );

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		initialize( sfb, metadata );
		configureSessionFactoryBuilder( sfb );

		sessionFactory = (SessionFactoryImplementor) sfb.build();
		afterSessionFactoryBuilt( sessionFactory );
	}

	protected final StandardServiceRegistryBuilder constructStandardServiceRegistryBuilder() {
		final BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder();
		// by default we do not share the BootstrapServiceRegistry nor the StandardServiceRegistry,
		// so we want the BootstrapServiceRegistry to be automatically closed when the
		// StandardServiceRegistry is closed.
		bsrb.enableAutoClose();
		configureBootstrapServiceRegistryBuilder( bsrb );

		final BootstrapServiceRegistry bsr = bsrb.build();
		afterBootstrapServiceRegistryBuilt( bsr );

		final Map settings = new HashMap();
		addSettings( settings );

		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder( bsr );
		initialize( ssrb );
		ssrb.applySettings( settings );
		configureStandardServiceRegistryBuilder( ssrb );
		return ssrb;
	}

	protected void addSettings(Map settings) {
	}

	/**
	 * Apply any desired config to the BootstrapServiceRegistryBuilder to be incorporated
	 * into the built BootstrapServiceRegistry
	 *
	 * @param bsrb The BootstrapServiceRegistryBuilder
	 */
	@SuppressWarnings({"SpellCheckingInspection", "UnusedParameters"})
	protected void configureBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder bsrb) {
	}

	/**
	 * Hook to allow tests to use the BootstrapServiceRegistry if they wish
	 *
	 * @param bsr The BootstrapServiceRegistry
	 */
	@SuppressWarnings("UnusedParameters")
	protected void afterBootstrapServiceRegistryBuilt(BootstrapServiceRegistry bsr) {
	}

	@SuppressWarnings("SpellCheckingInspection")
	private void initialize(StandardServiceRegistryBuilder ssrb) {
		final Dialect dialect = BaseCoreFunctionalTestCase.getDialect();

		ssrb.applySetting( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		ssrb.applySetting( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		if ( createSchema() ) {
			ssrb.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
			final String secondSchemaName = createSecondSchema();
			if ( StringHelper.isNotEmpty( secondSchemaName ) ) {
				if ( !H2Dialect.class.isInstance( dialect ) ) {
					// while it may be true that only H2 supports creation of a second schema via
					// URL (no idea whether that is accurate), every db should support creation of schemas
					// via DDL which SchemaExport can create for us.  See how this is used and
					// whether that usage could not just leverage that capability
					throw new UnsupportedOperationException( "Only H2 dialect supports creation of second schema." );
				}
				Helper.createH2Schema( secondSchemaName, ssrb.getSettings() );
			}
		}
		ssrb.applySetting( AvailableSettings.DIALECT, dialect.getClass().getName() );
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
	 * @param ssrb The StandardServiceRegistryBuilder
	 */
	@SuppressWarnings({"SpellCheckingInspection", "UnusedParameters"})
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
	}

	/**
	 * Hook to allow tests to use the StandardServiceRegistry if they wish
	 *
	 * @param ssr The StandardServiceRegistry
	 */
	@SuppressWarnings("UnusedParameters")
	protected void afterStandardServiceRegistryBuilt(StandardServiceRegistry ssr) {
	}

	protected void applyMetadataSources(MetadataSources metadataSources) {
		for ( String mapping : getMappings() ) {
			metadataSources.addResource( getBaseForMappings() + mapping );
		}

		for ( Class annotatedClass : getAnnotatedClasses() ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}

		for ( String annotatedPackage : getAnnotatedPackages() ) {
			metadataSources.addPackage( annotatedPackage );
		}

		for ( String ormXmlFile : getXmlFiles() ) {
			metadataSources.addInputStream( Thread.currentThread().getContextClassLoader().getResourceAsStream( ormXmlFile ) );
		}
	}

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	protected static final Class[] NO_CLASSES = new Class[0];

	protected Class[] getAnnotatedClasses() {
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

	private void initialize(MetadataBuilder metadataBuilder) {
		metadataBuilder.enableNewIdentifierGeneratorSupport( true );
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
		if ( !overrideCacheStrategy() ) {
			return;
		}

		if ( getCacheConcurrencyStrategy() == null ) {
			return;
		}

		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding.isInherited() ) {
				continue;
			}

			boolean hasLob = false;

			final Iterator props = entityBinding.getPropertyClosureIterator();
			while ( props.hasNext() ) {
				final Property prop = (Property) props.next();
				if ( prop.getValue().isSimpleValue() ) {
					if ( isLob( ( (SimpleValue) prop.getValue() ).getTypeName() ) ) {
						hasLob = true;
						break;
					}
				}
			}

			if ( !hasLob ) {
				( ( RootClass) entityBinding ).setCacheConcurrencyStrategy( getCacheConcurrencyStrategy() );
			}
		}

		for ( Collection collectionBinding : metadata.getCollectionBindings() ) {
			boolean isLob = false;

			if ( collectionBinding.getElement().isSimpleValue() ) {
				isLob = isLob( ( (SimpleValue) collectionBinding.getElement() ).getTypeName() );
			}

			if ( !isLob ) {
				collectionBinding.setCacheConcurrencyStrategy( getCacheConcurrencyStrategy() );
			}
		}
	}

	private boolean isLob(String typeName) {
		return "blob".equals( typeName )
				|| "clob".equals( typeName )
				|| "nclob".equals( typeName )
				|| Blob.class.getName().equals( typeName )
				|| Clob.class.getName().equals( typeName )
				|| NClob.class.getName().equals( typeName )
				|| BlobType.class.getName().equals( typeName )
				|| ClobType.class.getName().equals( typeName )
				|| NClobType.class.getName().equals( typeName );
	}

	protected void afterMetadataBuilt(Metadata metadata) {
	}

	private void initialize(SessionFactoryBuilder sfb, Metadata metadata) {
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
	@SuppressWarnings( {"UnusedDeclaration"})
	protected void shutDown() {
		releaseResources();
	}

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
	@SuppressWarnings( {"UnusedDeclaration"})
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

}
