/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import org.hibernate.envers.event.AuditEventListener;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.reader.AuditReaderImpl;
import static org.hibernate.envers.tools.ArraysTools.arrayIncludesInstanceOf;

import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventListeners;
import org.hibernate.event.PostInsertEventListener;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditReaderFactory {
    private AuditReaderFactory() { }

    /**
     * Create an audit reader associated with an open session.
     * @param session An open session.
     * @return An audit reader associated with the given sesison. It shouldn't be used
     * after the session is closed.
     * @throws AuditException When the given required listeners aren't installed.
     */
    public static AuditReader get(Session session) throws AuditException {
        SessionImplementor sessionImpl;
		if (!(session instanceof SessionImplementor)) {
			sessionImpl = (SessionImplementor) session.getSessionFactory().getCurrentSession();
		} else {
			sessionImpl = (SessionImplementor) session;
		}

        EventListeners listeners = sessionImpl.getListeners();

        for (PostInsertEventListener listener : listeners.getPostInsertEventListeners()) {
            if (listener instanceof AuditEventListener) {
                if (arrayIncludesInstanceOf(listeners.getPostUpdateEventListeners(), AuditEventListener.class) &&
                        arrayIncludesInstanceOf(listeners.getPostDeleteEventListeners(), AuditEventListener.class)) {
                    return new AuditReaderImpl(((AuditEventListener) listener).getVerCfg(), session,
                            sessionImpl);
                }
            }
        }

        throw new AuditException("You need to install the org.hibernate.envers.event.AuditEventListener " +
                "class as post insert, update and delete event listener.");
    }

    /**
     * Create an audit reader associated with an open entity manager.
     * @param entityManager An open entity manager.
     * @return An audit reader associated with the given entity manager. It shouldn't be used
     * after the entity manager is closed.
     * @throws AuditException When the given entity manager is not based on Hibernate, or if the required
     * listeners aren't installed.
     */
    public static AuditReader get(EntityManager entityManager) throws AuditException {
        if (entityManager.getDelegate() instanceof Session) {
            return get((Session) entityManager.getDelegate());
        }

        if (entityManager.getDelegate() instanceof EntityManager) {
            return get((EntityManager) entityManager.getDelegate());
        }

        throw new AuditException("Hibernate EntityManager not present!");
    }
}
