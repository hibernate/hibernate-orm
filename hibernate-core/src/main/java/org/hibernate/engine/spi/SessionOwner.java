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
 *
 * @deprecated (since 5.2) since consolidating hibernate-entitymanager into hibernate-core
 * I believe this is no longer needed.
 */
@Deprecated
public interface SessionOwner {
	/**
	 * Should session automatically be closed afterQuery transaction completion?
	 *
	 * @return {@literal true}/{@literal false} appropriately.
	 */
	boolean shouldAutoCloseSession();

	ExceptionMapper getExceptionMapper();

	AfterCompletionAction getAfterCompletionAction();

	ManagedFlushChecker getManagedFlushChecker();
}
