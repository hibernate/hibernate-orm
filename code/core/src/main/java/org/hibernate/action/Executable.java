//$Id: Executable.java 6607 2005-04-29 15:26:11Z oneovthafew $
package org.hibernate.action;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * An operation which may be scheduled for later execution.
 * Usually, the operation is a database insert/update/delete,
 * together with required second-level cache management.
 * 
 * @author Gavin King
 */
public interface Executable {
	/**
	 * Called before executing any actions
	 */
	public void beforeExecutions() throws HibernateException;
	/**
	 * Execute this action
	 */
	public void execute() throws HibernateException;
	/**
	 * Do we need to retain this instance until after the
	 * transaction completes?
	 * @return false if this class defines a no-op
	 * <tt>hasAfterTransactionCompletion()</tt>
	 */
	public boolean hasAfterTransactionCompletion();
	/**
	 * Called after the transaction completes
	 */
	public void afterTransactionCompletion(boolean success) throws HibernateException;
	/**
	 * What spaces (tables) are affected by this action?
	 */
	public Serializable[] getPropertySpaces();
}
