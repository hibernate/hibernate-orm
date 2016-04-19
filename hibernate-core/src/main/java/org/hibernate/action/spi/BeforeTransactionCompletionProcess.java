/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.spi;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Contract representing some process that needs to occur during beforeQuery transaction completion.
 *
 * @author Steve Ebersole
 */
public interface BeforeTransactionCompletionProcess {
	/**
	 * Perform whatever processing is encapsulated here beforeQuery completion of the transaction.
	 *
	 * @param session The session on which the transaction is preparing to complete.
	 */
	public void doBeforeTransactionCompletion(SessionImplementor session);
}
