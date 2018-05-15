/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.envers;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import org.jboss.logging.Logger;

import org.hibernate.testing.junit5.SessionFactoryAccess;

/**
 * A scope or holder for the SessionFactory instance associated with a given test class.
 * Used to:
 * <ul>
 *     <li>Provide lifecycle management related to the SessionFactory</li>
 * </ul>
 *
 * @author Chris Cranford
 */
public class EnversSessionFactoryScope implements SessionFactoryAccess {
	private static final Logger log = Logger.getLogger( EnversSessionFactoryScope.class );

	private final EnversSessionFactoryProducer sessionFactoryProducer;
	private final Strategy auditStrategy;

	private SessionFactory sessionFactory;

	public EnversSessionFactoryScope(EnversSessionFactoryProducer producer, Strategy auditStrategy) {
		log.debugf( "#<init> - %s", auditStrategy.getDisplayName() );
		this.auditStrategy = auditStrategy;
		this.sessionFactoryProducer = producer;
	}

	public void releaseSessionFactory() {
		log.debugf( "#releaseSessionFactory - %s", auditStrategy.getDisplayName() );
		if ( sessionFactory != null ) {
			log.infof( "Closing SessionFactory %s (%s)", sessionFactory, auditStrategy.getDisplayName() );
			sessionFactory.close();
			sessionFactory = null;
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		log.debugf( "#getSessionFactory - %s", auditStrategy.getDisplayName() );
		if ( sessionFactory == null || sessionFactory.isClosed() ) {
			sessionFactory = sessionFactoryProducer.produceSessionFactory( auditStrategy.getSettingValue() );
		}
		return (SessionFactoryImplementor) sessionFactory;
	}

	/**
	 * Invoke a lambda action inside an open session without a transaction-scope.
	 *
	 * The session will be closed after the action completes, regardless of failure.
	 *
	 * @param action The lambda action to invoke.
	 */
	public void inSession(Consumer<SessionImplementor> action) {
		inSession( getSessionFactory(), action );
	}

	/**
	 * Invoke a lambda action with an open session without a transaction-scope.
	 *
	 * The session will be closed after the action completes, regardless of failure.
	 *
	 * @param sfi The session factory.
	 * @param action The lambda action to invoke.
	 */
	public void inSession(SessionFactoryImplementor sfi, Consumer<SessionImplementor> action) {
		try ( SessionImplementor session = (SessionImplementor) sfi.openSession() ) {
			action.accept( session );
		}
	}

	public <R> R inSession(Function<SessionImplementor, R> action) {
		return inSession( getSessionFactory(), action );
	}

	public <R> R inSession(SessionFactoryImplementor sfi, Function<SessionImplementor, R> action) {
		try ( SessionImplementor session = (SessionImplementor) sfi.openSession() ) {
			return action.apply( session );
		}
	}

	/**
	 * Invoke a lambda action with an open session inside a new transaction.
	 *
	 * The session will be closed after the action completes, regardless of failure.
	 * The transaction will be committed if successful; otherwise it will be rolled back.
	 *
	 * @param action The lambda action to invoke.
	 */
	public void inTransaction(Consumer<SessionImplementor> action) {
		inTransaction( getSessionFactory(), action );
	}

	/**
	 * Invoke a lambda action with an open session inside a new transaction.
	 *
	 * The session will be closed after the action completes, regardless of failure.
	 * The transaction will be committed if successful; otherwise it will be rolled back.
	 *
	 * @param sfi The session factory.
	 * @param action The lambda action to invoke.
	 */
	public void inTransaction(SessionFactoryImplementor sfi, Consumer<SessionImplementor> action) {
		try ( SessionImplementor session = (SessionImplementor) sfi.openSession() ) {
			inTransaction( session, action );
		}
	}

	/**
	 * Invoke a lambda action inside a new transaction but bound to an existing open session.
	 *
	 * The session will not be closed after the action completes, it will be left as-is.
	 * The transaction will be committed if successful; otherwise it will be rolled back.
	 *
	 * @param session The session to bind the transaction against.
	 * @param action The lambda action to invoke.
	 */
	public void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
		final Transaction trx = session.beginTransaction();
		try {
			action.accept( session );
			trx.commit();
		}
		catch ( Exception e ) {
			try {
				trx.rollback();
			}
			catch ( Exception ignored ) {
			}
			throw e;
		}
	}

	public <R> R inTransaction(Function<SessionImplementor, R> action) {
		return inTransaction( getSessionFactory(), action );
	}

	public <R> R inTransaction(SessionFactoryImplementor sfi, Function<SessionImplementor, R> action) {
		try ( SessionImplementor session = (SessionImplementor) sfi.openSession() ) {
			return inTransaction( session, action );
		}
	}

	public <R> R inTransaction(SessionImplementor session, Function<SessionImplementor, R> action) {
		final Transaction trx = session.beginTransaction();
		try {
			R result = action.apply( session );
			trx.commit();
			return result;
		}
		catch ( Exception e ) {
			try {
				trx.rollback();
			}
			catch ( Exception ignored ) {

			}
			throw e;
		}
	}

	/**
	 * Invoke a series of lambda actions bound inside their own transaction-scope but bound to the same session.
	 *
	 * The session will be closed after the actions complete, regardless of failure.
	 * The transaction associated with lambda will be committed if successful; otherwise it will be rolled back.
	 * Should a lambda action fail, the remaining actions will be skipped.
	 *
	 * @param actions The lambda actions to invoke.
	 */
	public void inTransactions(Consumer<SessionImplementor>... actions) {
		try( SessionImplementor session = (SessionImplementor) getSessionFactory().openSession() ) {
			for ( Consumer<SessionImplementor> action : actions ) {
				inTransaction( session, action );
			}
		}
	}

	/**
	 * Invoke a lambda action against an {@link AuditReader} bound to a newly allocated session.
	 *
	 * The audit reader instance will be automatically allocated and provided to the lambda.
	 * The underlying session will be automatically opened and closed.
	 *
	 * @param action The lambda action to invoke.
	 */
	public void inAuditReader(Consumer<AuditReader> action) {
		try ( SessionImplementor session = (SessionImplementor) getSessionFactory().openSession() ) {
			AuditReader auditReader = AuditReaderFactory.get( session );
			action.accept( auditReader );
		}
	}
}
