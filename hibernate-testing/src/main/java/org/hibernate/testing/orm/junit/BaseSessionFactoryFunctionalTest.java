/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

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

	protected static final Class[] NO_CLASSES = new Class[0];
	private static final Logger log = Logger.getLogger( BaseSessionFactoryFunctionalTest.class );

	private ServiceRegistryScope registryScope;
	private DomainModelScope modelScope;
	private SessionFactoryScope sessionFactoryScope;

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
		applySettings( ssrBuilder );
		return ssrBuilder.build();
	}

	protected boolean exportSchema() {
		return true;
	}

	protected void applySettings(StandardServiceRegistryBuilder builer) {
	}

	@Override
	public void injectServiceRegistryScope(ServiceRegistryScope registryScope) {
		this.registryScope = registryScope;
	}

	@Override
	public MetadataImplementor produceModel(StandardServiceRegistry serviceRegistry) {
		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		applyMetadataSources( metadataSources );
		return (MetadataImplementor) metadataSources.buildMetadata();
	}

	protected void applyMetadataSources(MetadataSources metadataSources) {
		for ( Class annotatedClass : getAnnotatedClasses() ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
	}

	protected Class[] getAnnotatedClasses() {
		return NO_CLASSES;
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
		final SessionFactoryImplementor factory = (SessionFactoryImplementor) model.buildSessionFactory();
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


}
