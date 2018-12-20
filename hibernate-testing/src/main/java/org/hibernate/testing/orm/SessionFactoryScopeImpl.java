/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.util.function.Consumer;

import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryScopeImpl implements SessionFactoryScope {
	private static final Logger log = Logger.getLogger( SessionFactoryScopeImpl.class );

	private final MetadataImplementor metamodel;
	private final SessionFactory sessionFactoryConfig;

	private SessionFactoryImplementor sessionFactory;

	private boolean active = true;

	public SessionFactoryScopeImpl(MetadataImplementor metamodel, SessionFactory sessionFactoryConfig) {
		this.metamodel = metamodel;
		this.sessionFactoryConfig = sessionFactoryConfig;

		this.sessionFactory = createSessionFactory();
	}

	public void release() {
		if ( sessionFactory != null ) {
			try {
				sessionFactory.close();
			}
			catch (Exception e) {
				log.warn( "Error closing SF", e );
			}
		}

		active = false;
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

		try {
			final SessionFactoryBuilder sessionFactoryBuilder = metamodel.getSessionFactoryBuilder();

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
	}

	public void inSession(Consumer<SessionImplementor> action) {
		log.trace( "#inSession(action)" );
		inSession( getSessionFactory(), action );
	}

	public void inTransaction(Consumer<SessionImplementor> action) {
		log.trace( "#inTransaction(action)" );
		inTransaction( getSessionFactory(), action );
	}

	public void inSession(SessionFactoryImplementor sfi, Consumer<SessionImplementor> action) {
		log.trace( "##inSession(SF,action)" );

		try (SessionImplementor session = (SessionImplementor) sfi.openSession()) {
			log.trace( "Session opened, calling action" );
			action.accept( session );
			log.trace( "called action" );
		}
		finally {
			log.trace( "Session close - auto-close lock" );
		}
	}

	public void inTransaction(SessionFactoryImplementor factory, Consumer<SessionImplementor> action) {
		log.trace( "#inTransaction(factory, action)");


		try (SessionImplementor session = (SessionImplementor) factory.openSession()) {
			log.trace( "Session opened, calling action" );
			inTransaction( session, action );
			log.trace( "called action" );
		}
		finally {
			log.trace( "Session close - auto-close lock" );
		}
	}

	public void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
		log.trace( "inTransaction(session,action)" );

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
}
