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
