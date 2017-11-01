/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;


import java.util.function.Consumer;

import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import org.jboss.logging.Logger;

/**
 * A scope or holder fot the SessionFactory instance associated with a
 * given test class.  Used to:
 *
 * 		* provide lifecycle management related to the SessionFactory
 * 		* access to functional programming using a Session generated
 * 			from that SessionFactory
 *
 * @author Steve Ebersole
 */
public class SessionFactoryScope implements SessionFactoryAccess {
	private static final Logger log = Logger.getLogger( SessionFactoryScope.class );

	private final SessionFactoryProducer producer;

	private SessionFactoryImplementor sessionFactory;

	public SessionFactoryScope(SessionFactoryProducer producer) {
		log.trace( "SessionFactoryScope#<init>" );
		this.producer = producer;
	}

	public void rebuild() {
		log.trace( "SessionFactoryScope#rebuild" );
		releaseSessionFactory();

		sessionFactory = producer.produceSessionFactory();
	}

	public void releaseSessionFactory() {
		log.trace( "SessionFactoryScope#releaseSessionFactory" );
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		log.trace( "SessionFactoryScope#getSessionFactory" );
		if ( sessionFactory == null || sessionFactory.isClosed() ) {
			sessionFactory = producer.produceSessionFactory();
		}
		return sessionFactory;
	}

	public void inSession(Consumer<SessionImplementor> action) {
		log.trace( "  >> SessionFactoryScope#inSession" );

		final SessionImplementor session = (SessionImplementor) getSessionFactory().openSession();
		log.trace( "  >> SessionFactoryScope - Session opened" );

		try {
			log.trace( "    >> SessionFactoryScope - calling action" );
			action.accept( session );
			log.trace( "    >> SessionFactoryScope - called action" );
		}
		finally {
			log.trace( "  >> SessionFactoryScope - closing Session" );
			session.close();
		}
	}

	public void inTransaction(Consumer<SessionImplementor> action) {
		log.trace( "  >> SessionFactoryScope#inTransaction[not-passed-session]" );

		final SessionImplementor session = (SessionImplementor) getSessionFactory().openSession();
		log.trace( "  >> SessionFactoryScope - Session opened" );

		try {
			log.trace( "    >> SessionFactoryScope - calling action" );
			inTransaction( session, action );
			log.trace( "    >> SessionFactoryScope - called action" );
		}
		finally {
			log.trace( "  >> SessionFactoryScope - closing Session" );
			session.close();
		}
	}

	public void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
		log.trace( "  >> SessionFactoryScope#inTransaction[passed-session]" );

		final Transaction txn = session.beginTransaction();
		log.trace( "  >> SessionFactoryScope - Began transaction" );

		try {
			log.trace( "    >> SessionFactoryScope - calling action (in txn)" );
			action.accept( session );
			log.trace( "    >> SessionFactoryScope - called action (in txn)" );
		}
		catch (Exception e) {
			log.tracef(
					"    >> SessionFactoryScope - error calling action: %s (%s)",
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
		finally {
			try {
				log.trace( "  >> SessionFactoryScope - committing transaction" );
				txn.commit();
				log.trace( "  >> SessionFactoryScope - committing transaction" );
			}
			catch (Exception ignore) {
				// by definition a failed commit should rollback, so nothing more to do here
			}
		}
	}
}
