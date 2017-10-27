/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;


import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;

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
	private final SessionFactoryProducer producer;

	private SessionFactoryImplementor sessionFactory;

	public SessionFactoryScope(SessionFactoryProducer producer) {
		System.out.println( "SessionFactoryScope#<init>" );
		this.producer = producer;
	}

	public void rebuild() {
		System.out.println( "SessionFactoryScope#rebuild" );
		releaseSessionFactory();

		sessionFactory = producer.produceSessionFactory();
	}

	public void releaseSessionFactory() {
		System.out.println( "SessionFactoryScope#releaseSessionFactory" );
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		System.out.println( "SessionFactoryScope#getSessionFactory" );
		if ( sessionFactory == null ) {
			sessionFactory = producer.produceSessionFactory();
		}
		return sessionFactory;
	}

	public void withSession(Consumer<Session> action) {
		System.out.println( "  >> SessionFactoryScope#withSession" );

		final Session session = getSessionFactory().openSession();
		System.out.println( "  >> SessionFactoryScope - Session opened" );

		try {
			System.out.println( "    >> SessionFactoryScope - calling action" );
			action.accept( session );
			System.out.println( "    >> SessionFactoryScope - called action" );
		}
		finally {
			System.out.println( "  >> SessionFactoryScope - closing Session" );
			session.close();
		}
	}
}
