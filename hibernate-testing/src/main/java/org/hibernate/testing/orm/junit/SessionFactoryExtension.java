/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/// JUnit Jupiter [extension][org.junit.jupiter.api.extension.Extension] to support
/// SessionFactory-based functional testing.
///
/// @see SessionFactoryScope
/// @see DomainModelExtension
///
/// @implNote Leverages the [domain model][DomainModelScope] defined using the [DomainModelExtension].
///
/// @author Steve Ebersole
/// @author inpink
public class SessionFactoryExtension
		implements TestInstancePostProcessor, BeforeAllCallback, BeforeEachCallback,
		AfterEachCallback, AfterAllCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( SessionFactoryExtension.class );
	private static final String SESSION_FACTORY_KEY = SessionFactoryScope.class.getName();
	private static final String DROP_DATA_TIMING_KEY = "DROP_DATA_TIMING";

	/**
	 * Intended for use from external consumers.  Will never create a scope, just
	 * attempt to consume an already created and stored one
	 */
	public static SessionFactoryScope findSessionFactoryScope(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final SessionFactoryScope existing = (SessionFactoryScope) store.get( SESSION_FACTORY_KEY );
		if ( existing != null ) {
			return existing;
		}

		throw new RuntimeException( "Could not locate SessionFactoryScope : " + context.getDisplayName() );
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		log.tracef( "#postProcessTestInstance(%s, %s)", testInstance, context.getDisplayName() );

		final Optional<SessionFactory> sfAnnRef = AnnotationSupport.findAnnotation(
				context.getRequiredTestClass(),
				SessionFactory.class
		);

		if ( sfAnnRef.isPresent()
				|| SessionFactoryProducer.class.isAssignableFrom( context.getRequiredTestClass() ) ) {
			final DomainModelScope domainModelScope = DomainModelExtension.getOrCreateDomainModelScope( testInstance, context );
			final SessionFactoryScope created = createSessionFactoryScope( testInstance, sfAnnRef, domainModelScope, context );
			final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
			store.put( SESSION_FACTORY_KEY, created );

			final DropDataTiming[] dropDataTimings = resolveDropDataTimings( testInstance, sfAnnRef );
			store.put( DROP_DATA_TIMING_KEY, dropDataTimings );
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		handleDropData(context, DropDataTiming.BEFORE_EACH);

		final Optional<SessionFactory> sfAnnRef = AnnotationSupport.findAnnotation(
				context.getRequiredTestMethod(),
				SessionFactory.class
		);

		if ( sfAnnRef.isEmpty() ) {
			// assume the annotations are defined on the class-level...
			// will be validated by the parameter-resolver or SFS-extension
			return;
		}

		final DomainModelScope domainModelScope = DomainModelExtension.resolveForMethodLevelSessionFactoryScope( context );
		final SessionFactoryScope created = createSessionFactoryScope( context.getRequiredTestInstance(), sfAnnRef, domainModelScope, context );
		final ExtensionContext.Store extensionStore = locateExtensionStore( context.getRequiredTestInstance(), context );
		final DropDataTiming[] dropDataTimings = resolveDropDataTimings( context.getRequiredTestInstance(), sfAnnRef );
		extensionStore.put( DROP_DATA_TIMING_KEY, dropDataTimings );
		extensionStore.put( SESSION_FACTORY_KEY, created );
	}

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( SessionFactoryExtension.class, context, testInstance );
	}

	private static SessionFactoryScopeImpl createSessionFactoryScope(
			Object testInstance,
			Optional<SessionFactory> sfAnnRef,
			DomainModelScope domainModelScope,
			ExtensionContext context) {
		SessionFactoryProducer producer = null;

		if ( testInstance instanceof SessionFactoryProducer ) {
			producer = (SessionFactoryProducer) testInstance;
		}
		else {
			if ( context.getElement().isEmpty() ) {
				throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
			}

			if ( sfAnnRef.isPresent() ) {
				final SessionFactory sessionFactoryConfig = sfAnnRef.get();

				producer = model -> {
					try {
						final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
						if ( StringHelper.isNotEmpty( sessionFactoryConfig.sessionFactoryName() ) ) {
							sessionFactoryBuilder.applyName( sessionFactoryConfig.sessionFactoryName() );
						}

						if ( sessionFactoryConfig.generateStatistics() ) {
							sessionFactoryBuilder.applyStatisticsSupport( true );
						}

						if ( ! sessionFactoryConfig.interceptorClass().equals( Interceptor.class ) ) {
							sessionFactoryBuilder.applyInterceptor( sessionFactoryConfig.interceptorClass().getDeclaredConstructor().newInstance() );
						}

						final Class<? extends StatementInspector> explicitInspectorClass = sessionFactoryConfig.statementInspectorClass();
						if ( sessionFactoryConfig.useCollectingStatementInspector() ) {
							sessionFactoryBuilder.applyStatementInspector( new SQLStatementInspector() );
						}
						else if ( ! explicitInspectorClass.equals( StatementInspector.class ) ) {
							sessionFactoryBuilder.applyStatementInspector( explicitInspectorClass.getConstructor().newInstance() );
						}
						sessionFactoryBuilder.applyCollectionsInDefaultFetchGroup( sessionFactoryConfig.applyCollectionsInDefaultFetchGroup() );

						sessionFactoryConfig.sessionFactoryConfigurer().getDeclaredConstructor().newInstance()
								.accept( sessionFactoryBuilder );

						final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) sessionFactoryBuilder.build();
						if ( sessionFactoryConfig.exportSchema() ) {
							prepareSchemaExport( sessionFactory, model, sessionFactoryConfig.createSecondarySchemas() );
						}

						return sessionFactory;
					}
					catch (Exception e) {
						throw new RuntimeException( "Could not build SessionFactory: " + e.getMessage(), e );
					}
				};
			}
		}

		if ( producer == null ) {
			throw new IllegalStateException( "Could not determine SessionFactory producer" );
		}

		final SessionFactoryScopeImpl sfScope = new SessionFactoryScopeImpl( domainModelScope, producer );

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( sfScope );
		}

		return sfScope;
	}

	private static DropDataTiming[] resolveDropDataTimings(
			Object testInstance,
			Optional<SessionFactory> sfAnnRef) {
		if ( testInstance instanceof SessionFactoryProducer ) {
			return ((SessionFactoryProducer) testInstance).dropTestData();
		}
		else if ( sfAnnRef.isPresent() ) {
			return sfAnnRef.get().dropTestData();
		}
		else {
			return new DropDataTiming[]{};
		}
	}

	private static void prepareSchemaExport(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor model,
			boolean createSecondarySchemas) {
		final Map<String, Object> baseProperties = sessionFactory.getProperties();

		final Set<ActionGrouping> groupings = ActionGrouping.interpret( model, baseProperties );

		// if there are explicit setting for auto schema tooling then skip the annotation
		if ( ! groupings.isEmpty() ) {
			// the properties contained explicit settings for auto schema tooling - skip the annotation
			return;
		}

		final HashMap<String,Object> settings = new HashMap<>( baseProperties );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP );
		if ( createSecondarySchemas ) {
			if ( !( model.getDatabase().getDialect().canCreateSchema() ) ) {
				throw new UnsupportedOperationException(
						model.getDatabase().getDialect() + " does not support schema creation" );
			}
			settings.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, true );
		}
		final StandardServiceRegistry serviceRegistry = model.getMetadataBuildingOptions().getServiceRegistry();

		SchemaManagementToolCoordinator.process(
				model,
				serviceRegistry,
				settings,
				(action) -> sessionFactory.addObserver(
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
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.tracef( "#handleTestExecutionException(%s, %s)", context.getDisplayName(), throwable );

		try {
			final Object testInstance = context.getRequiredTestInstance();
			final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
			final SessionFactoryScopeImpl scope = (SessionFactoryScopeImpl) store.get( SESSION_FACTORY_KEY );
			scope.releaseSessionFactory();
		}
		catch (Exception ignore) {
		}

		throw throwable;
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		handleDropData(context, DropDataTiming.BEFORE_ALL);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		handleDropData(context, DropDataTiming.AFTER_EACH);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		handleDropData(context, DropDataTiming.AFTER_ALL);
	}

	private void handleDropData(ExtensionContext context, DropDataTiming timing) {
		try {
			final Object testInstance = context.getRequiredTestInstance();
			final ExtensionContext.Store store = locateExtensionStore(testInstance, context);

			final DropDataTiming[] configuredTimings = (DropDataTiming[]) store.get( DROP_DATA_TIMING_KEY );

			for (DropDataTiming configuredTiming : configuredTimings) {
				if (configuredTiming == timing) {
					final SessionFactoryScope scope = findSessionFactoryScope(testInstance, context);
					scope.dropData();
					log.debugf("Dropped data at timing %s for %s", timing, context.getDisplayName());
					break;
				}
			}
		}
		catch (Exception e) {
			log.warnf("Failed to drop data at timing %s: %s", timing, e);
		}
	}

	private static class SessionFactoryScopeImpl implements SessionFactoryScope, AutoCloseable {
		private final DomainModelScope modelScope;
		private final SessionFactoryProducer producer;

		private SessionFactoryImplementor sessionFactory;
		private boolean active = true;

		private SessionFactoryScopeImpl(
				DomainModelScope modelScope,
				SessionFactoryProducer producer) {
			this.modelScope = modelScope;
			this.producer = producer;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			if ( sessionFactory == null ) {
				sessionFactory = createSessionFactory();
			}

			return sessionFactory;
		}

		@Override
		public MetadataImplementor getMetadataImplementor() {
			return modelScope.getDomainModel();
		}

		@Override
		public StatementInspector getStatementInspector() {
			return getSessionFactory().getSessionFactoryOptions().getStatementInspector();
		}

		@Override
		public <T extends StatementInspector> T getStatementInspector(Class<T> type) {
			//noinspection unchecked
			return (T) getStatementInspector();
		}

		@Override
		public SQLStatementInspector getCollectingStatementInspector() {
			return getStatementInspector( SQLStatementInspector.class );
		}

		@Override
		public void close() {
			if ( !active ) {
				return;
			}

			log.debug( "Closing SessionFactoryScope" );

			active = false;
			releaseSessionFactory();
		}

		@Override
		public void releaseSessionFactory() {
			if ( sessionFactory != null ) {
				log.debug( "Releasing SessionFactory" );

				try {
					sessionFactory.close();
				}
				catch (Exception e) {
					log.warn( "Error closing SF", e );
				}
				finally {
					sessionFactory = null;
				}
			}
		}

		private SessionFactoryImplementor createSessionFactory() {
			if ( !active ) {
				throw new IllegalStateException( "SessionFactoryScope is no longer active" );
			}

			log.debug( "Creating SessionFactory" );

			return producer.produceSessionFactory( modelScope.getDomainModel() );
		}

		public void inSession(Consumer<SessionImplementor> action) {
			log.trace( "#inSession(Consumer)" );

			try (SessionImplementor session = getSessionFactory().openSession()) {
				log.trace( "Session opened, calling action" );
				action.accept( session );
			}
			finally {
				log.trace( "Session close - auto-close block" );
			}
		}

		@Override
		public <T> T fromSession(Function<SessionImplementor, T> action) {
			log.trace( "#fromSession(Function)" );

			try (SessionImplementor session = getSessionFactory().openSession()) {
				log.trace( "Session opened, calling action" );
				return action.apply( session );
			}
			finally {
				log.trace( "Session close - auto-close block" );
			}
		}

		@Override
		public void inTransaction(Consumer<SessionImplementor> action) {
			log.trace( "#inTransaction(Consumer)" );

			try (SessionImplementor session = getSessionFactory().openSession()) {
				log.trace( "Session opened, calling action" );
				inTransaction( session, action );
			}
			finally {
				log.trace( "Session close - auto-close block" );
			}
		}

		@Override
		public <T> T fromTransaction(Function<SessionImplementor, T> action) {
			log.trace( "#fromTransaction(Function)" );

			try (SessionImplementor session = getSessionFactory().openSession()) {
				log.trace( "Session opened, calling action" );
				return fromTransaction( session, action );
			}
			finally {
				log.trace( "Session close - auto-close block" );
			}
		}

		@Override
		public void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
			log.trace( "inTransaction(Session,Consumer)" );
			TransactionUtil.inTransaction( session, action );
		}

		@Override
		public void inTransaction(Function<SessionFactoryImplementor, SessionImplementor> sessionProducer, Consumer<SessionImplementor> action) {
			log.trace( "inTransaction(Function,Consumer)" );

			try (SessionImplementor session = sessionProducer.apply( getSessionFactory() )) {
				TransactionUtil.inTransaction( session, action );
			}
		}

		@Override
		public <T> T fromTransaction(SessionImplementor session, Function<SessionImplementor, T> action) {
			log.trace( "fromTransaction(Session,Function)" );
			return TransactionUtil.fromTransaction( session, action );
		}

		@Override
		public <T> T fromTransaction(Function<SessionFactoryImplementor, SessionImplementor> sessionProducer, Function<SessionImplementor, T> action) {
			log.trace( "fromTransaction(Function,Function)" );

			try (SessionImplementor session = sessionProducer.apply( getSessionFactory() )) {
				return TransactionUtil.fromTransaction( session, action );
			}
		}

		@Override
		public void inStatelessSession(Consumer<StatelessSessionImplementor> action) {
			log.trace( "#inStatelessSession(Consumer)" );

			try ( final StatelessSession statelessSession = getSessionFactory().openStatelessSession() ) {
				log.trace( "StatelessSession opened, calling action" );
				action.accept( (StatelessSessionImplementor) statelessSession );
			}
			finally {
				log.trace( "StatelessSession close - auto-close block" );
			}
		}

		@Override
		public void inStatelessTransaction(Consumer<StatelessSessionImplementor> action) {
			log.trace( "#inStatelessTransaction(Consumer)" );

			try ( final StatelessSession statelessSession = getSessionFactory().openStatelessSession() ) {
				log.trace( "StatelessSession opened, calling action" );
				inStatelessTransaction( (StatelessSessionImplementor) statelessSession, action );
			}
			finally {
				log.trace( "StatelessSession close - auto-close block" );
			}
		}

		@Override
		public void inStatelessTransaction(StatelessSessionImplementor session, Consumer<StatelessSessionImplementor> action) {
			log.trace( "inStatelessTransaction(StatelessSession,Consumer)" );

			TransactionUtil.inTransaction( session, action );
		}

		@Override
		public void dropData() {
			if ( sessionFactory != null ) {
				sessionFactory.getSchemaManager().truncateMappedObjects();
			}
		}
	}
}
