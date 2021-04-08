/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.query.sqm.mutation.internal.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.idtable.LocalTemporaryTableStrategy;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;

import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoImpl;
import org.hibernate.testing.orm.jpa.NonStringValueSettingProvider;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

import org.jboss.logging.Logger;

/**
 * hibernate-testing implementation of a few JUnit5 contracts to support SessionFactory-based testing,
 * including argument injection (or see {@link SessionFactoryScopeAware})
 *
 * @see SessionFactoryScope
 * @see DomainModelExtension
 *
 * @author Steve Ebersole
 */
public class EntityManagerFactoryExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( EntityManagerFactoryExtension.class );
	private static final String EMF_KEY = EntityManagerFactoryScope .class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( EntityManagerFactoryExtension.class, context, testInstance );
	}

	@SuppressWarnings("WeakerAccess")
	public static EntityManagerFactoryScope findEntityManagerFactoryScope(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final EntityManagerFactoryScope  existing = (EntityManagerFactoryScope ) store.get( EMF_KEY );
		if ( existing != null ) {
			return existing;
		}

		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		final Optional<Jpa> emfAnnWrapper = AnnotationSupport.findAnnotation(
				context.getElement().get(),
				Jpa.class
		);
		final Jpa emfAnn = emfAnnWrapper.orElseThrow( () -> new RuntimeException( "Could not locate @EntityManagerFactory" ) );

		final PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( emfAnn.persistenceUnitName() );

		pui.setTransactionType( emfAnn.transactionType() );
		pui.setCacheMode( emfAnn.sharedCacheMode() );
		pui.setValidationMode( emfAnn.validationMode() );
		pui.setExcludeUnlistedClasses( emfAnn.excludeUnlistedClasses() );

		final Setting[] properties = emfAnn.properties();
		for ( int i = 0; i < properties.length; i++ ) {
			final Setting property = properties[ i ];
			pui.getProperties().setProperty( property.name(), property.value() );
		}

		pui.getProperties().setProperty( AvailableSettings.GENERATE_STATISTICS, Boolean.toString( emfAnn.generateStatistics() ) );

		if ( emfAnn.exportSchema() ) {
			pui.getProperties().setProperty( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP.getExternalHbm2ddlName() );
		}

		if ( emfAnn.annotatedPackageNames().length > 0 ) {
			pui.applyManagedClassNames( emfAnn.annotatedPackageNames() );
		}

		if ( emfAnn.annotatedClassNames().length > 0 ) {
			pui.applyManagedClassNames( emfAnn.annotatedClassNames() );
		}

		if ( emfAnn.annotatedClasses().length > 0 ) {
			for ( int i = 0; i < emfAnn.annotatedClasses().length; i++ ) {
				pui.applyManagedClassNames( emfAnn.annotatedClasses()[i].getName() );
			}
		}

		if ( emfAnn.xmlMappings().length > 0 ) {
			pui.applyMappingFiles( emfAnn.xmlMappings() );
		}

		if ( emfAnn.standardModels().length > 0 ) {
			for ( int i = 0; i < emfAnn.standardModels().length; i++ ) {
				final StandardDomainModel standardDomainModel = emfAnn.standardModels()[ i ];
				for ( int i1 = 0; i1 < standardDomainModel.getDescriptor().getAnnotatedClasses().length; i1++ ) {
					final Class<?> annotatedClass = standardDomainModel.getDescriptor().getAnnotatedClasses()[ i1 ];
					pui.applyManagedClassNames( annotatedClass.getName() );
				}
			}
		}

		if ( emfAnn.modelDescriptorClasses().length > 0 ) {
			for ( int i = 0; i < emfAnn.modelDescriptorClasses().length; i++ ) {
				final Class<? extends DomainModelDescriptor> modelDescriptorClass = emfAnn.modelDescriptorClasses()[ i ];
				final DomainModelDescriptor domainModelDescriptor = instantiateDomainModelDescriptor( modelDescriptorClass );
				for ( int i1 = 0; i1 < domainModelDescriptor.getAnnotatedClasses().length; i1++ ) {
					final Class<?> annotatedClass = domainModelDescriptor.getAnnotatedClasses()[ i1 ];
					pui.applyManagedClassNames( annotatedClass.getName() );
				}
			}
		}

		final Map<String, Object> integrationSettings = new HashMap<>();
		integrationSettings.put( GlobalTemporaryTableStrategy.DROP_ID_TABLES, "true" );
		integrationSettings.put( LocalTemporaryTableStrategy.DROP_ID_TABLES, "true" );
		if ( !integrationSettings.containsKey( Environment.CONNECTION_PROVIDER ) ) {
			integrationSettings.put(
					AvailableSettings.CONNECTION_PROVIDER,
					SharedDriverManagerConnectionProviderImpl.getInstance()
			);
		}
		for ( int i = 0; i < emfAnn.integrationSettings().length; i++ ) {
			final Setting setting = emfAnn.integrationSettings()[ i ];
			integrationSettings.put( setting.name(), setting.value() );
		}

		if ( emfAnn.nonStringValueSettingProviders().length > 0 ) {
			for ( int i = 0; i < emfAnn.nonStringValueSettingProviders().length; i++ ) {
				final Class<? extends NonStringValueSettingProvider> _class = emfAnn.nonStringValueSettingProviders()[ i ];
				try {
					NonStringValueSettingProvider valueProvider = _class.newInstance();
					integrationSettings.put( valueProvider.getKey(), valueProvider.getValue() );
				}
				catch (Exception e) {
					log.error( "Error obtaining special value for " + _class.getName(), e );
				}
			}
		}

		final EntityManagerFactoryScopeImpl scope = new EntityManagerFactoryScopeImpl( pui, integrationSettings );

		locateExtensionStore( testInstance, context ).put( EMF_KEY, scope );

		return scope;
	}

	private static DomainModelDescriptor instantiateDomainModelDescriptor(Class<? extends DomainModelDescriptor> modelDescriptorClass) {
		// first, see if it has a static singleton reference and use that if so
		try {
			final Field[] declaredFields = modelDescriptorClass.getDeclaredFields();
			for ( int i = 0; i < declaredFields.length; i++ ) {
				final Field field = declaredFields[ i ];
				if ( ReflectHelper.isStaticField( field ) ) {
					final Object value = field.get( null );
					if ( value instanceof DomainModelDescriptor ) {
						return (DomainModelDescriptor) value;
					}
				}
			}
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException( "Problem accessing DomainModelDescriptor fields : " + modelDescriptorClass.getName(), e );
		}

		// no singleton field, try to instantiate it via reflection
		try {
			return modelDescriptorClass.getConstructor( null ).newInstance( null );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException( "Problem instantiation DomainModelDescriptor : " + modelDescriptorClass.getName(), e );
		}
	}

	private static void prepareSchemaExport(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor model) {
		final Map<String, Object> baseProperties = sessionFactory.getProperties();

		final Set<ActionGrouping> groupings = ActionGrouping.interpret( model, baseProperties );
		if ( ! groupings.isEmpty() ) {
			// the properties contained explicit settings for auto schema tooling - skip the annotation
			return;
		}

		final HashMap settings = new HashMap<>( baseProperties );
		//noinspection unchecked
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP );

		final StandardServiceRegistry serviceRegistry = model.getMetadataBuildingOptions().getServiceRegistry();


		SchemaManagementToolCoordinator.process(
				model,
				serviceRegistry,
				settings,
				action -> sessionFactory.addObserver(
						new SessionFactoryObserver() {
							@Override
							public void sessionFactoryClosing(org.hibernate.SessionFactory factory) {
								action.perform( serviceRegistry );
							}
						}
				)
		);
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		log.tracef( "#postProcessTestInstance(%s, %s)", testInstance, context.getDisplayName() );

		findEntityManagerFactoryScope( testInstance, context );
	}

	@Override
	public void afterAll(ExtensionContext context) {
		log.tracef( "#afterAll(%s)", context.getDisplayName() );

		final Object testInstance = context.getRequiredTestInstance();

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( null );
		}

		final EntityManagerFactoryScopeImpl removed = (EntityManagerFactoryScopeImpl) locateExtensionStore( testInstance, context ).remove( EMF_KEY );
		if ( removed != null ) {
			removed.close();
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.tracef( "#handleTestExecutionException(%s, %s)", context.getDisplayName(), throwable );

		try {
			final Object testInstance = context.getRequiredTestInstance();
			final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
			final EntityManagerFactoryScopeImpl scope = (EntityManagerFactoryScopeImpl) store.get( EMF_KEY );
			scope.releaseEntityManagerFactory();
		}
		catch (Exception ignore) {
		}

		throw throwable;
	}

	private static class EntityManagerFactoryScopeImpl implements EntityManagerFactoryScope, ExtensionContext.Store.CloseableResource {
		private final PersistenceUnitInfo persistenceUnitInfo;
		private final Map<String, Object> integrationSettings;

		private javax.persistence.EntityManagerFactory emf;
		private boolean active = true;

		private EntityManagerFactoryScopeImpl(
				PersistenceUnitInfo persistenceUnitInfo,
				Map<String, Object> integrationSettings) {
			this.persistenceUnitInfo = persistenceUnitInfo;
			this.integrationSettings = integrationSettings;
		}

		@Override
		public javax.persistence.EntityManagerFactory getEntityManagerFactory() {
			if ( emf == null ) {
				emf = createEntityManagerFactory();
			}

			return emf;
		}

		@Override
		public StatementInspector getStatementInspector() {
			return getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getSessionFactoryOptions().getStatementInspector();
		}

		@Override
		public void close() {
			if ( ! active ) {
				return;
			}

			log.debug( "Closing SessionFactoryScope" );

			active = false;
			releaseEntityManagerFactory();
		}

		public void releaseEntityManagerFactory() {
			if ( emf != null ) {
				log.debug( "Releasing SessionFactory" );

				try {
					emf.close();
				}
				catch (Exception e) {
					log.warn( "Error closing EMF", e );
				}
				finally {
					emf = null;
				}
			}
		}

		private javax.persistence.EntityManagerFactory createEntityManagerFactory() {
			if ( ! active ) {
				throw new IllegalStateException( "EntityManagerFactoryScope is no longer active" );
			}

			log.debug( "Creating EntityManagerFactory" );

			final EntityManagerFactoryBuilder emfBuilder = Bootstrap.getEntityManagerFactoryBuilder(
					new PersistenceUnitInfoDescriptor( persistenceUnitInfo ),
					integrationSettings
			);

			return emfBuilder.build();
		}

		@Override
		public void inEntityManager(Consumer<EntityManager> action) {
			log.trace( "#inEntityManager(Consumer)" );

			try (SessionImplementor session = getEntityManagerFactory().createEntityManager().unwrap( SessionImplementor.class ) ) {
				log.trace( "EntityManager opened, calling action" );
				action.accept( session );
			}
			finally {
				log.trace( "EntityManager close - auto-close block" );
			}
		}

		@Override
		public <T> T fromEntityManager(Function<EntityManager, T> action) {
			log.trace( "#fromEntityManager(Function)" );

			try (SessionImplementor session = getEntityManagerFactory().createEntityManager().unwrap( SessionImplementor.class ) ) {
				log.trace( "EntityManager opened, calling action" );
				return action.apply( session );
			}
			finally {
				log.trace( "EntityManager close - auto-close block" );
			}
		}

		@Override
		public void inTransaction(Consumer<EntityManager> action) {
			log.trace( "#inTransaction(Consumer)" );

			try (SessionImplementor session = getEntityManagerFactory().createEntityManager().unwrap( SessionImplementor.class ) ) {
				log.trace( "EntityManager opened, calling action" );
				inTransaction( session, action );
			}
			finally {
				log.trace( "EntityManager close - auto-close block" );
			}
		}

		@Override
		public <T> T fromTransaction(Function<EntityManager, T> action) {
			log.trace( "#fromTransaction(Function)" );

			try (SessionImplementor session = getEntityManagerFactory().createEntityManager().unwrap( SessionImplementor.class ) ) {
				log.trace( "EntityManager opened, calling action" );
				return fromTransaction( session, action );
			}
			finally {
				log.trace( "EntityManager close - auto-close block" );
			}
		}

		@Override
		public void inTransaction(EntityManager entityManager, Consumer<EntityManager> action) {
			log.trace( "inTransaction(EntityManager,Consumer)" );

			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final Transaction txn = session.beginTransaction();
			log.trace( "Started transaction" );

			try {
				log.trace( "Calling action in txn" );
				action.accept( session );
				log.trace( "Called action - in txn" );

				if ( !txn.getRollbackOnly() ) {
					log.trace( "Committing transaction" );
					txn.commit();
					log.trace( "Committed transaction" );
				}
				else {
					try {
						log.trace( "Rollback transaction marked for rollback only" );
						txn.rollback();
					}
					catch (Exception e) {
						log.error( "Rollback failure", e );
					}
				}
			}
			catch (Exception e) {
				log.tracef(
						"Error calling action: %s (%s) - rolling back",
						e.getClass().getName(),
						e.getMessage()
				);
				try {
					txn.rollback();
				}
				catch (Exception ignore) {
					log.trace( "Was unable to roll back transaction" );
					// really nothing else we can do here - the attempt to
					//		rollback already failed and there is nothing else
					// 		to clean up.
				}

				throw e;
			}
			catch (AssertionError t) {
				try {
					txn.rollback();
				}
				catch (Exception ignore) {
					log.trace( "Was unable to roll back transaction" );
					// really nothing else we can do here - the attempt to
					//		rollback already failed and there is nothing else
					// 		to clean up.
				}
				throw t;
			}
		}

		@Override
		public <T> T fromTransaction(EntityManager entityManager, Function<EntityManager,T> action) {
			log.trace( "fromTransaction(EntityManager,Function)" );

			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final Transaction txn = session.beginTransaction();
			log.trace( "Started transaction" );

			try {
				log.trace( "Calling action in txn" );
				final T result = action.apply( session );
				log.trace( "Called action - in txn" );

				log.trace( "Committing transaction" );
				txn.commit();
				log.trace( "Committed transaction" );

				return result;
			}
			catch (Exception e) {
				log.tracef(
						"Error calling action: %s (%s) - rolling back",
						e.getClass().getName(),
						e.getMessage()
				);
				try {
					txn.rollback();
				}
				catch (Exception ignore) {
					log.trace( "Was unable to roll back transaction" );
					// really nothing else we can do here - the attempt to
					//		rollback already failed and there is nothing else
					// 		to clean up.
				}

				throw e;
			}
			catch (AssertionError t) {
				try {
					txn.rollback();
				}
				catch (Exception ignore) {
					log.trace( "Was unable to roll back transaction" );
					// really nothing else we can do here - the attempt to
					//		rollback already failed and there is nothing else
					// 		to clean up.
				}
				throw t;
			}
		}
	}
}
