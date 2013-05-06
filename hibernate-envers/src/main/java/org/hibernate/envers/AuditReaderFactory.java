/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.event.spi.EnversListener;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.reader.AuditReaderImpl;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEventListener;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditReaderFactory {
	private AuditReaderFactory() {
	}

	/**
	 * Create an audit reader associated with an open session.
	 *
	 * @param session An open session.
	 *
	 * @return An audit reader associated with the given sesison. It shouldn't be used
	 *         after the session is closed.
	 *
	 * @throws AuditException When the given required listeners aren't installed.
	 */
	public static AuditReader get(Session session) throws AuditException {
		SessionImplementor sessionImpl;
		if ( !(session instanceof SessionImplementor) ) {
			sessionImpl = (SessionImplementor) session.getSessionFactory().getCurrentSession();
		}
		else {
			sessionImpl = (SessionImplementor) session;
		}

		// todo : I wonder if there is a better means to do this via "named lookup" based on the session factory name/uuid
		final EventListenerRegistry listenerRegistry = sessionImpl
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class );

		for ( PostInsertEventListener listener : listenerRegistry.getEventListenerGroup( EventType.POST_INSERT )
				.listeners() ) {
			if ( listener instanceof EnversListener ) {
				// todo : slightly different from original code in that I am not checking the other listener groups...
				return new AuditReaderImpl(
						((EnversListener) listener).getAuditConfiguration(),
						session,
						sessionImpl
				);
			}
		}

		throw new AuditException( "Envers listeners were not properly registered" );
	}

	/**
	 * Create an audit reader associated with an open entity manager.
	 *
	 * @param entityManager An open entity manager.
	 *
	 * @return An audit reader associated with the given entity manager. It shouldn't be used
	 *         after the entity manager is closed.
	 *
	 * @throws AuditException When the given entity manager is not based on Hibernate, or if the required
	 * listeners aren't installed.
	 */
	public static AuditReader get(EntityManager entityManager) throws AuditException {
		if ( entityManager.getDelegate() instanceof Session ) {
			return get( (Session) entityManager.getDelegate() );
		}

		if ( entityManager.getDelegate() instanceof EntityManager ) {
			return get( (EntityManager) entityManager.getDelegate() );
		}

		throw new AuditException( "Hibernate EntityManager not present!" );
	}
}
