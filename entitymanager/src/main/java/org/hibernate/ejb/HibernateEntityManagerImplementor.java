/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.ejb;

import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;
import org.hibernate.LockOptions;

/**
 * Additional internal contracts for the Hibernate {@link javax.persistence.EntityManager} implementation.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface HibernateEntityManagerImplementor extends HibernateEntityManager {
	/**
	 * Get access to the Hibernate extended EMF contract.
	 *
	 * @return The Hibernate EMF contract for this EM.
	 */
	public HibernateEntityManagerFactory getFactory();

	/**
	 * Provides access to whether a transaction is currently in progress.
	 *
	 * @return True if a transaction is considered currently in progress; false otherwise.
	 */
	boolean isTransactionInProgress();

	/**
	 * Handles marking for rollback and other such operations that need to occur depending on the type of
	 * exception being handled.
	 *
	 * @param e The exception being handled.
	 */
	public void handlePersistenceException(PersistenceException e);

	/**
	 * Delegates to {@link #handlePersistenceException} and then throws the given exception.
	 *
	 * @param e The exception being handled and finally thrown.
	 */
	public void throwPersistenceException(PersistenceException e);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception; note that the JPA sepcification makes use
	 * of exceptions outside its exception hierarchy, though they are all runtime exceptions.
	 * <p/>
	 * Any appropriate/needed calls to {@link #handlePersistenceException} are also made.
	 *
	 * @param e The Hibernate excepton.
	 * @param lockOptions The lock options in effect at the time of exception (can be null)
	 * @return The JPA-specified exception
	 */
	public RuntimeException convert(HibernateException e, LockOptions lockOptions);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception; note that the JPA sepcification makes use
	 * of exceptions outside its exception hierarchy, though they are all runtime exceptions.
	 * <p/>
	 * Any appropriate/needed calls to {@link #handlePersistenceException} are also made.
	 *
	 * @param e The Hibernate excepton.
	 * @return The JPA-specified exception
	 */
	public RuntimeException convert(HibernateException e);

	/**
	 * Delegates to {@link #convert} and then throws the given exception.
	 *
	 * @param e The exception being handled and finally thrown.
	 */
	public void throwPersistenceException(HibernateException e);

	public PersistenceException wrapStaleStateException(StaleStateException e);
}
