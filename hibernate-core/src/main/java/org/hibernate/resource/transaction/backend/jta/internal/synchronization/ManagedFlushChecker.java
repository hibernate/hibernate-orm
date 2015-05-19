/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import java.io.Serializable;

import org.hibernate.internal.SessionImpl;

/**
 * A pluggable strategy for defining how the {@link javax.transaction.Synchronization} registered by Hibernate determines
 * whether to perform a managed flush.  An exceptions from either this delegate or the subsequent flush are routed
 * through the sister strategy {@link ExceptionMapper}.
 *
 * @author Steve Ebersole
 */
public interface ManagedFlushChecker extends Serializable {
	/**
	 * Check whether we should perform the managed flush
	 *
	 * @param session The Session
	 *
	 * @return True to indicate to perform the managed flush; false otherwise.
	 */
	public boolean shouldDoManagedFlush(SessionImpl session);
}
