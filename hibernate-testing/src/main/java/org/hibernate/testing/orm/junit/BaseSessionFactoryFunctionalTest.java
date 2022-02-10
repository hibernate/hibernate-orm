/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;

import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;

import org.jboss.logging.Logger;

/**
 * Template (GoF pattern) based abstract class for tests bridging the legacy
 * approach of SessionFactory building as a test fixture
 *
 * @author Steve Ebersole
 */
@SessionFactoryFunctionalTesting
public abstract class BaseSessionFactoryFunctionalTest
		implements ServiceRegistryProducer, ServiceRegistryScopeAware,
		DomainModelProducer, DomainModelScopeAware,
		SessionFactoryProducer, SessionFactoryScopeAware {

	protected static final Dialect DIALECT = DialectContext.getDialect();

	protected static final Class[] NO_CLASSES = new Class[0];
	protected static final String[] NO_MAPPINGS = new String[0];

	private static final Logger log = Logger.getLogger( BaseSessionFactoryFunctionalTest.class );

	private ServiceRegistryScope registryScope;
	private DomainModelScope modelScope;
	private SessionFactoryScope sessionFactoryScope;

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	protected SessionFactoryScope sessionFactoryScope() {
		return sessionFactoryScope;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactoryScope.getSessionFactory();
	}

	protected MetadataImplementor getMetadata(){
		return modelScope.getDomainModel();
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder ssrBuilder) {
		ssrBuilder.applySetting( AvailableSettings.HBM2DDL_AUTO, exportSchema() ? "create-drop" : "none" );
		if ( !Environment.getProperties().containsKey( Environment.CONNECTION_PROVIDER ) ) {
			ssrBuilder.applySetting(
					AvailableSettings.CONNECTION_PROVIDER,
					SharedDriverManagerConnectionProviderImpl.getInstance()
			);
		}
		applySettings( ssrBuilder );
		return ssrBuilder.build();
	}

	@Override
	public void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder bsrb) {

	}

	protected boolean exportSchema() {
		return true;
	}

	protected void applySettings(StandardServiceRegistryBuilder builder) {
	}

	@Override
	public void injectServiceRegistryScope(ServiceRegistryScope registryScope) {
		this.registryScope = registryScope;
	}

	@Override
	public MetadataImplementor produceModel(StandardServiceRegistry serviceRegistry) {
		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		applyMetadataBuilder( metadataBuilder );
		applyMetadataSources( metadataSources );
		final MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();
		if ( !overrideCacheStrategy() || getCacheConcurrencyStrategy() == null ) {
			return metadata;
		}

		applyCacheSettings( metadata );

		return metadata;
	}

	protected final void applyCacheSettings(Metadata metadata) {
		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding.isInherited() ) {
				continue;
			}

			boolean hasLob = false;

			final Iterator props = entityBinding.getPropertyClosureIterator();
			while ( props.hasNext() ) {
				final Property prop = (Property) props.next();
				if ( prop.getValue().isSimpleValue() ) {
					if ( isLob( (SimpleValue) prop.getValue() ) ) {
						hasLob = true;
						break;
					}
				}
			}

			if ( !hasLob ) {
				( (RootClass) entityBinding ).setCacheConcurrencyStrategy( getCacheConcurrencyStrategy() );
				entityBinding.setCached( true );
			}
		}

		for ( Collection collectionBinding : metadata.getCollectionBindings() ) {
			boolean isLob = false;

			if ( collectionBinding.getElement().isSimpleValue() ) {
				isLob = isLob( (SimpleValue) collectionBinding.getElement() );
			}

			if ( !isLob ) {
				collectionBinding.setCacheConcurrencyStrategy( getCacheConcurrencyStrategy() );
			}
		}
	}

	protected boolean overrideCacheStrategy() {
		return true;
	}

	protected String getCacheConcurrencyStrategy() {
		return null;
	}

	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {

	}

	protected void applyMetadataSources(MetadataSources metadataSources) {

		for ( Class annotatedClass : getAnnotatedClasses() ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
		String[] xmlFiles = getOrmXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				try ( InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile ) ) {
					metadataSources.addInputStream( is );
				}
				catch (IOException e) {
					throw new IllegalArgumentException( e );
				}
			}
		}
	}

	protected Class[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getOrmXmlFiles() {
		return NO_MAPPINGS;
	}

	@Override
	public void injectTestModelScope(DomainModelScope modelScope) {
		this.modelScope = modelScope;
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		log.trace( "Producing SessionFactory" );
		final SessionFactoryBuilder sfBuilder = model.getSessionFactoryBuilder();
		configure( sfBuilder );
		final SessionFactoryImplementor factory = (SessionFactoryImplementor) sfBuilder.build();
		sessionFactoryBuilt( factory );
		return factory;
	}

	protected void configure(SessionFactoryBuilder builder) {
	}

	protected void sessionFactoryBuilt(SessionFactoryImplementor factory) {
	}

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		sessionFactoryScope = scope;
	}

	// there is a chicken-egg problem here where the
//	@AfterAll
//	public void dropDatabase() {
//		final SchemaManagementToolCoordinator.ActionGrouping actions = SchemaManagementToolCoordinator.ActionGrouping.interpret(
//				registry.getService( ConfigurationService.class ).getSettings()
//		);
//
//		final boolean needsDropped = this.model != null && ( exportSchema() || actions.getDatabaseAction() != Action.NONE );
//
//		if ( needsDropped ) {
//			// atm we do not expose the (runtime) DatabaseModel from the SessionFactory so we
//			// need to recreate it from the boot model.
//			//
//			// perhaps we should expose it from SF?
//			final DatabaseModel databaseModel = Helper.buildDatabaseModel( registry, model );
//			new SchemaExport( databaseModel, registry ).drop( EnumSet.of( TargetType.DATABASE ) );
//		}
//	}

	@AfterEach
	public final void afterTest() {
		if ( isCleanupTestDataRequired() ) {
			cleanupTestData();
		}
	}

	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	protected void cleanupTestData() {
		inTransaction(
				session ->
						getMetadata().getEntityBindings().forEach(
								entityType -> session.createQuery( "delete from " + entityType.getEntityName() ).executeUpdate()
						)

		);
	}

	protected void inTransaction(Consumer<SessionImplementor> action) {
		sessionFactoryScope().inTransaction( action );
	}

	protected <T> T fromTransaction(Function<SessionImplementor, T> action) {
		return sessionFactoryScope().fromTransaction( action );
	}

	protected void inSession(Consumer<SessionImplementor> action){
		sessionFactoryScope.inSession( action );
	}

	protected <T> T fromSession(Function<SessionImplementor, T> action){
		return sessionFactoryScope.fromSession( action );
	}

	protected Dialect getDialect(){
		return DialectContext.getDialect();
	}

	private static boolean isLob(SimpleValue value) {
		final String typeName = value.getTypeName();
		if ( typeName != null ) {
			String significantTypeNamePart = typeName.substring( typeName.lastIndexOf( '.' ) + 1 )
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

	protected Future<?> executeAsync(Runnable callable) {
		return executorService.submit(callable);
	}

	protected void executeSync(Runnable callable) {
		try {
			executeAsync( callable ).get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			throw new RuntimeException( e.getCause() );
		}
	}

	/**
	 * Execute function in a Hibernate transaction without return value
	 *
	 * @param sessionBuilderSupplier SessionFactory supplier
	 * @param function function
	 */
	public static void doInHibernateSessionBuilder(
			Supplier<SessionBuilder> sessionBuilderSupplier,
			TransactionUtil.HibernateTransactionConsumer function) {
		Session session = null;
		Transaction txn = null;
		try {
			session = sessionBuilderSupplier.get().openSession();
			function.beforeTransactionCompletion();
			txn = session.beginTransaction();

			function.accept( session );
			if ( !txn.getRollbackOnly() ) {
				txn.commit();
			}
			else {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
		}
		catch ( Throwable t ) {
			if ( txn != null && txn.isActive() ) {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
			throw t;
		}
		finally {
			function.afterTransactionCompletion();
			if ( session != null ) {
				session.close();
			}
		}
	}

}
