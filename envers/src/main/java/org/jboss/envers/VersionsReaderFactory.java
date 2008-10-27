/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers;

import org.hibernate.Session;
import org.hibernate.event.EventListeners;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.engine.SessionImplementor;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.event.VersionsEventListener;
import static org.jboss.envers.tools.ArraysTools.arrayIncludesInstanceOf;
import org.jboss.envers.reader.VersionsReaderImpl;

import javax.persistence.EntityManager;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsReaderFactory {
    private VersionsReaderFactory() { }

    /**
     * Create a versions reader associated with an open session.
     * <b>WARNING:</b> Using Envers with Hibernate (not with Hibernate Entity Manager/JPA) is experimental,
     * if possible, use {@link org.jboss.envers.VersionsReaderFactory#get(javax.persistence.EntityManager)}.
     * @param session An open session.
     * @return A versions reader associated with the given sesison. It shouldn't be used
     * after the session is closed.
     * @throws VersionsException When the given required listeners aren't installed.
     */
    public static VersionsReader get(Session session) throws VersionsException {
        SessionImplementor sessionImpl = (SessionImplementor) session;

        EventListeners listeners = sessionImpl.getListeners();

        for (PostInsertEventListener listener : listeners.getPostInsertEventListeners()) {
            if (listener instanceof VersionsEventListener) {
                if (arrayIncludesInstanceOf(listeners.getPostUpdateEventListeners(), VersionsEventListener.class) &&
                        arrayIncludesInstanceOf(listeners.getPostDeleteEventListeners(), VersionsEventListener.class)) {
                    return new VersionsReaderImpl(((VersionsEventListener) listener).getVerCfg(), session,
                            sessionImpl);
                }
            }
        }

        throw new VersionsException("You need install the org.jboss.envers.event.VersionsEventListener " +
                "class as post insert, update and delete event listener.");
    }

    /**
     * Create a versions reader associated with an open entity manager.
     * @param entityManager An open entity manager.
     * @return A versions reader associated with the given entity manager. It shouldn't be used
     * after the entity manager is closed.
     * @throws VersionsException When the given entity manager is not based on Hibernate, or if the required
     * listeners aren't installed.
     */
    public static VersionsReader get(EntityManager entityManager) throws VersionsException {
        if (entityManager.getDelegate() instanceof Session) {
            return get((Session) entityManager.getDelegate());
        }

        if (entityManager.getDelegate() instanceof EntityManager) {
            if (entityManager.getDelegate() instanceof Session) {
                return get((Session) entityManager.getDelegate());
            }
        }

        throw new VersionsException("Hibernate EntityManager not present!");
    }
}
