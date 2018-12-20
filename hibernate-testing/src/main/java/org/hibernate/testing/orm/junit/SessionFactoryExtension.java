/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( SessionFactoryExtension.class );
	private static final String SESSION_FACTORY_KEY = SessionFactoryScope.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( SessionFactoryExtension.class, context, testInstance );
	}

	@SuppressWarnings("WeakerAccess")
	public static SessionFactoryScope findSessionFactoryScope(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final SessionFactoryScope existing = (SessionFactoryScope) store.get( SESSION_FACTORY_KEY );
		if ( existing != null ) {
			return existing;
		}

		SessionFactoryProducer producer = null;

		if ( testInstance instanceof SessionFactoryProducer ) {
			producer = (SessionFactoryProducer) testInstance;
		}
		else {
			if ( !context.getElement().isPresent() ) {
				throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
			}

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

						sessionFactoryBuilder.applyStatisticsSupport( sessionFactoryConfig.generateStatistics() );

						if ( ! sessionFactoryConfig.interceptorClass().equals( Interceptor.class ) ) {
							sessionFactoryBuilder.applyInterceptor( sessionFactoryConfig.interceptorClass().newInstance() );
						}

						if ( ! sessionFactoryConfig.statementInspectorClass().equals( StatementInspector.class ) ) {
							sessionFactoryBuilder.applyStatementInspector(
									sessionFactoryConfig.statementInspectorClass().newInstance()
							);
						}

						return (SessionFactoryImplementor) sessionFactoryBuilder.build();
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
				DomainModelExtension.findMetamodelScope( testInstance, context ),
				producer
		);

		locateExtensionStore( testInstance, context ).put( SESSION_FACTORY_KEY, sfScope );

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( sfScope );
		}

		return sfScope;
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

			this.sessionFactory = createSessionFactory();
		}

		@Override
		public void close() {
			if ( ! active ) {
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

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			if ( sessionFactory == null ) {
				sessionFactory = createSessionFactory();
			}

			return sessionFactory;
		}

		private SessionFactoryImplementor createSessionFactory() {
			if ( ! active ) {
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

			final Transaction txn = session.beginTransaction();
			log.trace( "Started transaction" );

			try {
				log.trace( "Calling action in txn" );
				action.accept( session );
				log.trace( "Called action - in txn" );

				log.trace( "Committing transaction" );
				txn.commit();
				log.trace( "Committed transaction" );
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
		}

		@Override
		public <T> T fromTransaction(SessionImplementor session, Function<SessionImplementor,T> action) {
			log.trace( "fromTransaction(Session,Function)" );

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
		}
	}
}
