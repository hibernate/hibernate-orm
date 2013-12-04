/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.synchronization;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;

/**
 * Class responsible for evicting audit data entries that have been stored in the session level cache.
 * This operation increases Envers performance in case of massive entity updates without clearing persistence context.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SessionCacheCleaner {
	/**
	 * Schedules audit data removal from session level cache after transaction completion. The operation is performed
	 * regardless of commit success.
	 *
	 * @param session Active Hibernate session.
	 * @param data Audit data that shall be evicted (e.g. revision data or entity snapshot)
	 */
	public void scheduleAuditDataRemoval(final Session session, final Object data) {
		((EventSource) session).getActionQueue().registerProcess(
				new AfterTransactionCompletionProcess() {
					public void doAfterTransactionCompletion(boolean success, SessionImplementor sessionImplementor) {
						if ( !sessionImplementor.isClosed() ) {
							try {
								( (Session) sessionImplementor ).evict( data );
							}
							catch ( HibernateException ignore ) {
							}
						}
					}
				}
		);
	}
}
