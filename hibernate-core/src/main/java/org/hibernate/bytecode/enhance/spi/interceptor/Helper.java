/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Locale;

import org.hibernate.FlushMode;
import org.hibernate.LazyInitializationException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.SessionFactoryRegistry;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class Helper {
	private static final Logger log = Logger.getLogger( Helper.class );

	interface Consumer {
		SharedSessionContractImplementor getLinkedSession();
		boolean allowLoadOutsideTransaction();
		String getSessionFactoryUuid();
	}

	interface LazyInitializationWork<T> {
		T doWork(SharedSessionContractImplementor session, boolean isTemporarySession);

		// informational details
		String getEntityName();
		String getAttributeName();
	}


	private final Consumer consumer;

	public Helper(Consumer consumer) {
		this.consumer = consumer;
	}

	public <T> T performWork(LazyInitializationWork<T> lazyInitializationWork) {
		SharedSessionContractImplementor session = consumer.getLinkedSession();

		boolean isTempSession = false;
		boolean isJta = false;

		// first figure out which Session to use
		if ( session == null ) {
			if ( consumer.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( lazyInitializationWork );
				isTempSession = true;
			}
			else {
				throwLazyInitializationException( Cause.NO_SESSION, lazyInitializationWork );
			}
		}
		else if ( !session.isOpen() ) {
			if ( consumer.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( lazyInitializationWork );
				isTempSession = true;
			}
			else {
				throwLazyInitializationException( Cause.CLOSED_SESSION, lazyInitializationWork );
			}
		}
		else if ( !session.isConnected() ) {
			if ( consumer.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( lazyInitializationWork );
				isTempSession = true;
			}
			else {
				throwLazyInitializationException( Cause.DISCONNECTED_SESSION, lazyInitializationWork );
			}
		}

		// If we are using a temporary Session, begin a transaction if necessary
		if ( isTempSession ) {
			isJta = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

			if ( !isJta ) {
				// Explicitly handle the transactions only if we're not in
				// a JTA environment.  A lazy loading temporary session can
				// be created even if a current session and transaction are
				// open (ex: session.clear() was used).  We must prevent
				// multiple transactions.
				session.beginTransaction();
			}
		}

		try {
			// do the actual work
			return lazyInitializationWork.doWork( session, isTempSession );
		}
		finally {
			if ( isTempSession ) {
				try {
					// Commit the JDBC transaction is we started one.
					if ( !isJta ) {
						session.getTransaction().commit();
					}
				}
				catch (Exception e) {
					log.warn(
							"Unable to commit JDBC transaction on temporary session used to load lazy " +
									"collection associated to no session"
					);
				}

				// Close the just opened temp Session
				try {
					session.close();
				}
				catch (Exception e) {
					log.warn( "Unable to close temporary session used to load lazy collection associated to no session" );
				}
			}
		}
	}

	enum Cause {
		NO_SESSION,
		CLOSED_SESSION,
		DISCONNECTED_SESSION,
		NO_SF_UUID
	}

	private void throwLazyInitializationException(Cause cause, LazyInitializationWork work) {
		final String reason;
		switch ( cause ) {
			case NO_SESSION: {
				reason = "no session and settings disallow loading outside the Session";
				break;
			}
			case CLOSED_SESSION: {
				reason = "session is closed and settings disallow loading outside the Session";
				break;
			}
			case DISCONNECTED_SESSION: {
				reason = "session is disconnected and settings disallow loading outside the Session";
				break;
			}
			case NO_SF_UUID: {
				reason = "could not determine SessionFactory UUId to create temporary Session for loading";
				break;
			}
			default: {
				reason = "<should never get here>";
			}
		}

		final String message = String.format(
				Locale.ROOT,
				"Unable to perform requested lazy initialization [%s.%s] - %s",
				work.getEntityName(),
				work.getAttributeName(),
				reason
		);

		throw new LazyInitializationException( message );
	}

	private SharedSessionContractImplementor openTemporarySessionForLoading(LazyInitializationWork lazyInitializationWork) {
		if ( consumer.getSessionFactoryUuid() == null ) {
			throwLazyInitializationException( Cause.NO_SF_UUID, lazyInitializationWork );
		}

		final SessionFactoryImplementor sf = (SessionFactoryImplementor)
				SessionFactoryRegistry.INSTANCE.getSessionFactory( consumer.getSessionFactoryUuid() );
		final SharedSessionContractImplementor session = (SharedSessionContractImplementor) sf.openSession();
		session.getPersistenceContext().setDefaultReadOnly( true );
		session.setHibernateFlushMode( FlushMode.MANUAL );
		return session;
	}
}
