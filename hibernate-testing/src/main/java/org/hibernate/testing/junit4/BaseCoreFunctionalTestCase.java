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
package org.hibernate.testing.junit4;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.TruthValue;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.type.Type;

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
	private MetadataImplementor metadataImplementor;
	private final boolean isMetadataUsed;

	protected Session session;
	private List<Session> secondarySessions;

	protected static Dialect getDialect() {
		return DIALECT;
	}

	protected BaseCoreFunctionalTestCase() {
		// configure(Configuration) may add a setting for using the new metamodel, so,  for now,
		// build a dummy configuration to get all the property settings.
		final Configuration dummyConfiguration = constructAndConfigureConfiguration();

		// Can't build the ServiceRegistry until after the constructor executes
		// (otherwise integrators don't get added).
		// Create a dummy ConfigurationService just to find out if the new metamodel will be used.
		final ConfigurationService dummyConfigurationService = new ConfigurationServiceImpl(
				dummyConfiguration.getProperties()
		);
		isMetadataUsed = true;
	}

	protected Configuration configuration() {
		return configuration;
	}

	protected MetadataImplementor metadata() {
		return metadataImplementor;
	}

	protected final StandardServiceRegistryImpl serviceRegistry() {
		return serviceRegistry;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected Session openSession() throws HibernateException {
		registerPreviousSessionIfNeeded( session );
		session = sessionFactory().openSession();
		return session;
	}

	private void registerPreviousSessionIfNeeded(Session session) {
		if ( session == null ) {
			return;
		}

		if ( !session.isOpen() ) {
			return;
		}

		registerSecondarySession( session );
	}

	protected Session openSession(Interceptor interceptor) throws HibernateException {
		registerPreviousSessionIfNeeded( session );
		session = sessionFactory().withOptions().interceptor( interceptor ).openSession();
		return session;
	}

	protected Session openSecondarySession() throws HibernateException {
		Session session = sessionFactory().openSession();
		registerSecondarySession( session );
		return session;
	}

	protected void registerSecondarySession(Session session) {
		if ( secondarySessions == null ) {
			secondarySessions = new LinkedList<Session>();
		}
		secondarySessions.add( session );
	}


	// before/after test class ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@BeforeClassOnce
	@SuppressWarnings( {"UnusedDeclaration"})
	protected void buildSessionFactory() {
		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		configuration = constructAndConfigureConfiguration();
		serviceRegistry = buildServiceRegistry( bootRegistry, configuration );

		metadataImplementor = buildMetadata();
		afterConstructAndConfigureMetadata( metadataImplementor );
		final SessionFactoryBuilder sessionFactoryBuilder = metadataImplementor.getSessionFactoryBuilder();
		if ( configuration.getInterceptor() != null ) {
			sessionFactoryBuilder.with( configuration.getInterceptor() );
		}
		sessionFactory = (SessionFactoryImpl) sessionFactoryBuilder.build();

		afterSessionFactoryBuilt();
	}

	protected boolean isMetadataUsed() {
		return isMetadataUsed;
	}

	protected void rebuildSessionFactory() {
		try {
			releaseSessionFactory();
		}
		catch (Exception ignore) {
		}

		buildSessionFactory();
	}


	protected void afterConstructAndConfigureMetadata(MetadataImplementor metadataImplementor) {
		applyCacheSettings( metadataImplementor );
	}

	public void applyCacheSettings(MetadataImplementor metadataImplementor) {
		if ( !overrideCacheStrategy() || StringHelper.isEmpty( getCacheConcurrencyStrategy() ) ) {
			return;
		}

		overrideCacheSettings( metadataImplementor, getCacheConcurrencyStrategy() );
	}

	public static void overrideCacheSettings(MetadataImplementor metadata, String accessType) {
		for ( EntityBinding entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding.getSuperEntityBinding() == null ) {
				overrideEntityCache( entityBinding, accessType );
			}
			overrideCollectionCachesForEntity( entityBinding, accessType );
		}
	}

	private static void overrideEntityCache(EntityBinding entityBinding, String accessType) {
		boolean hasLob = false;
		for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
			if ( attributeBinding.getAttribute().isSingular() ) {
				Type type = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
				String typeName = type.getName();
				if ( "blob".equals( typeName ) || "clob".equals( typeName ) ) {
					hasLob = true;
					break;
				}
				if ( Blob.class.getName().equals( typeName ) || Clob.class.getName().equals( typeName ) ) {
					hasLob = true;
					break;
				}
			}
		}
		if ( !hasLob && entityBinding.getSuperEntityBinding() == null ) {
			entityBinding.getHierarchyDetails().getCaching().setRequested( TruthValue.TRUE );
			entityBinding.getHierarchyDetails().getCaching().setRegion( entityBinding.getEntityName() );
			entityBinding.getHierarchyDetails().getCaching().setCacheLazyProperties( true );
			entityBinding.getHierarchyDetails().getCaching().setAccessType( AccessType.fromExternalName( accessType ) );
		}
	}

	private static void overrideCollectionCachesForEntity(EntityBinding entityBinding, String accessType) {
		for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
			if ( !attributeBinding.getAttribute().isSingular() ) {
				AbstractPluralAttributeBinding binding = AbstractPluralAttributeBinding.class.cast( attributeBinding );

				binding.getCaching().setRequested( TruthValue.TRUE );
				binding.getCaching().setRegion(
						StringHelper.qualify(
								entityBinding.getEntityName(),
								attributeBinding.getAttribute().getName()
						)
				);
				binding.getCaching().setCacheLazyProperties( true );
				binding.getCaching().setAccessType( AccessType.fromExternalName( accessType ) );
			}
		}
	}

	private MetadataImplementor buildMetadata() {
		assert BootstrapServiceRegistry.class.isInstance( serviceRegistry.getParentServiceRegistry() );
		MetadataSources sources = new MetadataSources( serviceRegistry.getParentServiceRegistry() );
		addMappings( sources );
		return (MetadataImplementor) sources.getMetadataBuilder( serviceRegistry ).build();
	}

	protected Configuration constructAndConfigureConfiguration() {
		Configuration cfg = constructConfiguration();
		configure( cfg );
		return cfg;
	}

	protected Configuration constructConfiguration() {
		Configuration configuration = new Configuration()
				.setProperty(Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName()  );
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
		configuration.setProperty( Environment.DIALECT, getDialect().getClass().getName() );
		return configuration;
	}

	protected void configure(Configuration configuration) {
	}

	protected void addMappings(MetadataSources sources) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				sources.addResource(
						getBaseForMappings() + mapping
				);
			}
		}
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				sources.addAnnotatedClass( annotatedClass );
			}
		}
		String[] annotatedPackages = getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				sources.addPackage( annotatedPackage );
			}
		}
		String[] xmlFiles = getXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				sources.addInputStream( is );
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


	protected boolean overrideCacheStrategy() {
		return true;
	}

	protected String getCacheConcurrencyStrategy() {
		return null;
	}

	protected void afterConfigurationBuilt(Configuration configuration) {
//		afterConfigurationBuilt( configuration.createMappings(), getDialect() );
	}

	protected void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
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

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( bootRegistry ).applySettings( properties );
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
		if ( isCleanupTestDataRequired() ) {
			cleanupTestData();
		}
		cleanupTest();

		cleanupSession( session );
		if ( secondarySessions != null ) {
			for ( Session session: secondarySessions ) {
				cleanupSession( session );
			}
		}

		assertAllDataRemoved();

	}

	protected void cleanupCache() {
		if ( sessionFactory != null ) {
			sessionFactory.getCache().evictAllRegions();
		}
	}
	
	protected boolean isCleanupTestDataRequired() { return false; }
	
	protected void cleanupTestData() throws Exception {
		Session s = sessionFactory.openSession();
		try {
			s.beginTransaction();
			try {
				s.createQuery( "delete from java.lang.Object" ).executeUpdate();
				s.getTransaction().commit();
			}
			catch (Exception e) {
				try {
					s.doWork( new RollbackWork() );
				}
				catch (Exception ignore) {
				}
			}
		}
		finally {
			s.close();
		}
	}

	private void cleanupSession(Session session) {
		if ( session != null && ! ( (SessionImplementor) session ).isClosed() ) {
			if ( session.isConnected() ) {
				session.doWork( new RollbackWork() );
			}
			session.close();
		}
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
