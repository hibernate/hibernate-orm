/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.resource.transaction.backend.jta.internal.synchronization.AfterCompletionAction;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ManagedFlushChecker;

/**
 * The contract for a Session owner.  Typically this is something that wraps the Session.
 *
 * @author Gail Badner
 *
 * @see SessionBuilderImplementor#owner
 */
public interface SessionOwner {
	/**
	 * Should session automatically be closed after transaction completion?
	 *
	 * @return {@literal true}/{@literal false} appropriately.
	 */
	public boolean shouldAutoCloseSession();

	public ExceptionMapper getExceptionMapper();

	public AfterCompletionAction getAfterCompletionAction();

	public ManagedFlushChecker getManagedFlushChecker();
}
