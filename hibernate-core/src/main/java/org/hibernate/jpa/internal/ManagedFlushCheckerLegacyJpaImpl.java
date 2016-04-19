/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ManagedFlushChecker;

/**
 * @author Steve Ebersole
 */
public class ManagedFlushCheckerLegacyJpaImpl implements ManagedFlushChecker {
	/**
	 * Singleton access
	 */
	public static final ManagedFlushCheckerLegacyJpaImpl INSTANCE = new ManagedFlushCheckerLegacyJpaImpl();

	@Override
	public boolean shouldDoManagedFlush(SessionImplementor session) {
		return !session.isClosed()
				&& session.getHibernateFlushMode() == FlushMode.MANUAL;
	}
}
