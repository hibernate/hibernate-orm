/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.audit.internal.AuditLogImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Factory for obtaining {@link AuditLog} instances.
 * <p>
 * Two creation modes are supported:
 * <ul>
 *   <li><b>Standalone</b> (from {@link SessionFactory} or
 *       {@link EntityManagerFactory}): opens its own JDBC
 *       connection. Use when no session is available.</li>
 *   <li><b>Connection-sharing</b> (from {@link Session},
 *       {@link StatelessSession}, or {@link EntityManager}):
 *       shares the session's JDBC connection. Use when a
 *       session is already open.</li>
 * </ul>
 * The returned {@link AuditLog} is {@link AutoCloseable} and
 * should be closed by the caller to release the internal session.
 * For connection-sharing instances, the audit log is also
 * automatically closed when the parent session closes.
 *
 * @author Marco Belladelli
 * @see AuditLog
 * @since 7.4
 */
public final class AuditLogFactory {

	private AuditLogFactory() {
	}

	/**
	 * Create a standalone audit log.
	 *
	 * @param sessionFactory the session factory
	 * @return a new audit log (must be closed by the caller)
	 */
	public static AuditLog create(SessionFactory sessionFactory) {
		final var sf = (SessionFactoryImplementor) sessionFactory;
		final var session = (SharedSessionContractImplementor)
				sf.withOptions()
						.atChangeset( AuditLog.ALL_CHANGESETS )
						.openSession();
		return new AuditLogImpl( session );
	}

	/**
	 * Create a standalone audit log.
	 *
	 * @param entityManagerFactory the entity manager factory
	 * @return a new audit log (must be closed by the caller)
	 */
	public static AuditLog create(EntityManagerFactory entityManagerFactory) {
		return create( entityManagerFactory.unwrap( SessionFactory.class ) );
	}

	/**
	 * Create an audit log sharing the session's JDBC connection.
	 *
	 * @param session the session whose connection to share
	 * @return a new audit log (must be closed by the caller)
	 */
	public static AuditLog create(Session session) {
		return createFromSession( session );
	}

	/**
	 * Create an audit log sharing the stateless session's
	 * JDBC connection.
	 *
	 * @param session the stateless session whose connection to share
	 * @return a new audit log (must be closed by the caller)
	 */
	public static AuditLog create(StatelessSession session) {
		return createFromSession( session );
	}

	/**
	 * Create an audit log sharing the entity manager's
	 * JDBC connection.
	 *
	 * @param entityManager the entity manager whose connection to share
	 * @return a new audit log (must be closed by the caller)
	 */
	public static AuditLog create(EntityManager entityManager) {
		return createFromSession( entityManager.unwrap( Session.class ) );
	}

	private static AuditLog createFromSession(SharedSessionContract session) {
		final var childSession = (SharedSessionContractImplementor)
				session.sessionWithOptions()
						.connection()
						.atChangeset( AuditLog.ALL_CHANGESETS )
						.openSession();
		return new AuditLogImpl( childSession );
	}
}
