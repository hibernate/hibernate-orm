/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.spi;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * An operation which may be scheduled for later execution.  Usually, the operation is a database
 * insert/update/delete, together with required second-level cache management.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Executable {
	/**
	 * What spaces (tables) are affected by this action?
	 *
	 * @return The spaces affected by this action.
	 */
	Serializable[] getPropertySpaces();

	/**
	 * Called beforeQuery executing any actions.  Gives actions a chance to perform any preparation.
	 *
	 * @throws HibernateException Indicates a problem during preparation.
	 */
	void beforeExecutions() throws HibernateException;

	/**
	 * Execute this action.
	 *
	 * @throws HibernateException Indicates a problem during execution.
	 */
	void execute() throws HibernateException;

	/**
	 * Get the afterQuery-transaction-completion process, if any, for this action.
	 *
	 * @return The afterQuery-transaction-completion process, or null if we have no
	 * afterQuery-transaction-completion process
	 */
	AfterTransactionCompletionProcess getAfterTransactionCompletionProcess();

	/**
	 * Get the beforeQuery-transaction-completion process, if any, for this action.
	 *
	 * @return The beforeQuery-transaction-completion process, or null if we have no
	 * beforeQuery-transaction-completion process
	 */
	BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess();
	
	/**
	 * Reconnect to session afterQuery deserialization
	 *
	 * @param session The session being deserialized
	 */
	void afterDeserialize(SharedSessionContractImplementor session);
}
