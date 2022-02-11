/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.transaction.TransactionUtil;
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
public class SessionFactoryExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( SessionFactoryExtension.class );
	private static final String SESSION_FACTORY_KEY = SessionFactoryScope.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( SessionFactoryExtension.class, context, testInstance );
	}

	public static SessionFactoryScope findSessionFactoryScope(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final SessionFactoryScope existing = (SessionFactoryScope) store.get( SESSION_FACTORY_KEY );
		if ( existing != null ) {
			return existing;
		}

		SessionFactoryProducer producer = null;

		final DomainModelScope domainModelScope = DomainModelExtension.findDomainModelScope( testInstance, context );

		if ( testInstance instanceof SessionFactoryProducer ) {
			producer = (SessionFactoryProducer) testInstance;
		}
		else if ( ! context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}
		else {
			final Optional<SessionFactory> sfAnnWrappper = AnnotationSupport.findAnnotation(
					context.getElement().get(),
					SessionFactory.class
			);

			if ( sfAnnWrappper.isPresent() ) {
				final SessionFactory sessionFactoryConfig = sfAnnWrappper.get();

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
							sessionFactoryBuilder.applyInterceptor( sessionFactoryConfig.interceptorClass().newInstance() );
						}

						final Class<? extends StatementInspector> explicitInspectorClass = sessionFactoryConfig.statementInspectorClass();
						if ( sessionFactoryConfig.useCollectingStatementInspector() ) {
							sessionFactoryBuilder.applyStatementInspector( new SQLStatementInspector() );
						}
						else if ( ! explicitInspectorClass.equals( StatementInspector.class ) ) {
							sessionFactoryBuilder.applyStatementInspector( explicitInspectorClass.getConstructor().newInstance() );
						}

						final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) sessionFactoryBuilder.build();

						if ( sessionFactoryConfig.exportSchema() ) {
							prepareSchemaExport( sessionFactory, model, sessionFactoryConfig.createSecondarySchemas() );
						}

						return sessionFactory;
					}
					catch (Exception e) {
						throw new RuntimeException( "Could not build SessionFactory", e );
					}
				};
			}
		}

		if ( producer == null ) {
			throw new IllegalStateException( "Could not determine SessionFactory producer" );
		}

		final SessionFactoryScopeImpl sfScope = new SessionFactoryScopeImpl(
				domainModelScope,
				producer
		);

		locateExtensionStore( testInstance, context ).put( SESSION_FACTORY_KEY, sfScope );

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( sfScope );
		}

		return sfScope;
	}

	private static void prepareSchemaExport(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor model,
			boolean createSecondarySchemas) {
		final ActionGrouping grouping = ActionGrouping.interpret( sessionFactory.getProperties() );
		if ( grouping.getDatabaseAction() != Action.NONE || grouping.getScriptAction() != Action.NONE ) {
			// the properties contained explicit settings for auto schema tooling; skip here as part of
			// @SessionFactory handling
			return;
		}

		final HashMap<String,Object> settings = new HashMap<>( sessionFactory.getProperties() );
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

		findSessionFactoryScope( testInstance, context );
	}

	@Override
	public void afterAll(ExtensionContext context) {
		log.tracef( "#afterAll(%s)", context.getDisplayName() );

		final Object testInstance = context.getRequiredTestInstance();

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( null );
		}

		final SessionFactoryScopeImpl removed = (SessionFactoryScopeImpl) locateExtensionStore( testInstance, context ).remove( SESSION_FACTORY_KEY );
		if ( removed != null ) {
			removed.close();
		}

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( null );
		}
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

	private static class SessionFactoryScopeImpl implements SessionFactoryScope, ExtensionContext.Store.CloseableResource {
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

			try (SessionImplementor session = (SessionImplementor) getSessionFactory().openSession()) {
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

			try (SessionImplementor session = (SessionImplementor) getSessionFactory().openSession()) {
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

			try (SessionImplementor session = (SessionImplementor) getSessionFactory().openSession()) {
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

			try (SessionImplementor session = (SessionImplementor) getSessionFactory().openSession()) {
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
		public <T> T fromTransaction(SessionImplementor session, Function<SessionImplementor, T> action) {
			log.trace( "fromTransaction(Session,Function)" );
			return TransactionUtil.fromTransaction( session, action );
		}

		@Override
		public void inStatelessSession(Consumer<StatelessSession> action) {
			log.trace( "#inStatelessSession(Consumer)" );

			try ( final StatelessSession statelessSession = getSessionFactory().openStatelessSession() ) {
				log.trace( "StatelessSession opened, calling action" );
				action.accept( statelessSession );
			}
			finally {
				log.trace( "StatelessSession close - auto-close block" );
			}
		}

		@Override
		public void inStatelessTransaction(Consumer<StatelessSession> action) {
			log.trace( "#inStatelessTransaction(Consumer)" );

			try ( final StatelessSession statelessSession = getSessionFactory().openStatelessSession() ) {
				log.trace( "StatelessSession opened, calling action" );
				inStatelessTransaction( statelessSession, action );
			}
			finally {
				log.trace( "StatelessSession close - auto-close block" );
			}
		}

		@Override
		public void inStatelessTransaction(StatelessSession session, Consumer<StatelessSession> action) {
			log.trace( "inStatelessTransaction(StatelessSession,Consumer)" );

			TransactionUtil.inTransaction( session, action );
		}
	}
}
