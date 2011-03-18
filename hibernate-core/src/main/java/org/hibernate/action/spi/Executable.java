/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.action.spi;

import java.io.Serializable;

import org.hibernate.HibernateException;

/**
 * An operation which may be scheduled for later execution.
 * Usually, the operation is a database insert/update/delete,
 * together with required second-level cache management.
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
	public Serializable[] getPropertySpaces();

	/**
	 * Called before executing any actions.  Gives actions a chance to perform any preparation.
	 *
	 * @throws HibernateException Indicates a problem during preparation.
	 */
	public void beforeExecutions() throws HibernateException;

	/**
	 * Execute this action
	 *
	 * @throws HibernateException Indicates a problem during execution.
	 */
	public void execute() throws HibernateException;

	/**
	 * Get the after-transaction-completion process, if any, for this action.
	 *
	 * @return The after-transaction-completion process, or null if we have no
	 * after-transaction-completion process
	 */
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess();

	/**
	 * Get the before-transaction-completion process, if any, for this action.
	 *
	 * @return The before-transaction-completion process, or null if we have no
	 * before-transaction-completion process
	 */
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess();
}
