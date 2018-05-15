/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.reader.AuditReaderImpl;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
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
	 *         afterQuery the session is closed.
	 *
	 * @throws AuditException When the given required listeners aren't installed.
	 * @deprecated (since 6.0), use {@link SessionFactory#openAuditReader()}.
	 */
	@Deprecated
	public static AuditReader get(Session session) throws AuditException {
		return new AuditReaderImpl( session, false );
	}

	/**
	 * Create an audit reader associated with an open entity manager.
	 *
	 * @param entityManager An open entity manager.
	 *
	 * @return An audit reader associated with the given entity manager. It shouldn't be used
	 *         afterQuery the entity manager is closed.
	 *
	 * @throws AuditException When the given entity manager is not based on Hibernate, or if the required
	 * listeners aren't installed.
	 * @deprecated (since 6.0), use
	 * {@code EntityManager.unwrap(Session.class).getSessionFactory().openAuditReader()} or
	 * {@code EntityManager.getEntityManagerFactory().unwrap(SessionFactory.class).openAuditReader()}.
	 */
	@Deprecated
	public static AuditReader get(EntityManager entityManager) throws AuditException {
		return get( entityManager.unwrap( Session.class ) );
	}
}
