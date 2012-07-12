/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal;

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;

/**
 * @author Gavin King
 * @author Emmanuel Bernard
 */
public class TransactionImpl implements EntityTransaction {

	private HibernateEntityManagerImplementor entityManager;
	private Transaction tx;
	private boolean rollbackOnly;

	public TransactionImpl(AbstractEntityManagerImpl entityManager) {
		this.entityManager = entityManager;
	}

	private Session getSession() {
		return entityManager.getSession();
	}

	public void begin() {
		try {
			rollbackOnly = false;
			if ( tx != null && tx.isActive() ) {
				throw new IllegalStateException( "Transaction already active" );
			}
			//entityManager.adjustFlushMode();
			tx = getSession().beginTransaction();
		}
		catch (HibernateException he) {
			entityManager.throwPersistenceException( he );
		}
	}

	public void commit() {
		if ( tx == null || !tx.isActive() ) {
			throw new IllegalStateException( "Transaction not active" );
		}
		if ( rollbackOnly ) {
			tx.rollback();
			throw new RollbackException( "Transaction marked as rollbackOnly" );
		}
		try {
			tx.commit();
		}
		catch (Exception e) {
			Exception wrappedException;
			if (e instanceof HibernateException) {
				wrappedException = entityManager.convert( (HibernateException)e );
			}
			else {
				wrappedException = e;
			}
			try {
				//as per the spec we should rollback if commit fails
				tx.rollback();
			}
			catch (Exception re) {
				//swallow
			}
			throw new RollbackException( "Error while committing the transaction", wrappedException );
		}
		finally {
			rollbackOnly = false;
		}
		//if closed and we commit, the mode should have been adjusted already
		//if ( entityManager.isOpen() ) entityManager.adjustFlushMode();
	}

	public void rollback() {
		if ( tx == null || !tx.isActive() ) {
			throw new IllegalStateException( "Transaction not active" );
		}
		try {
			tx.rollback();
		}
		catch (Exception e) {
			throw new PersistenceException( "unexpected error when rollbacking", e );
		}
		finally {
			try {
				if (entityManager !=  null) {
					Session session = getSession();
					if ( session != null && session.isOpen() ) session.clear();
				}
			}
			catch (Throwable t) {
				//we don't really care here since it's only for safety purpose
			}
			rollbackOnly = false;
		}
	}

	public void setRollbackOnly() {
		if ( ! isActive() ) throw new IllegalStateException( "Transaction not active" );
		this.rollbackOnly = true;
	}

	public boolean getRollbackOnly() {
		if ( ! isActive() ) throw new IllegalStateException( "Transaction not active" );
		return rollbackOnly;
	}

	public boolean isActive() {
		try {
			return tx != null && tx.isActive();
		}
		catch (RuntimeException e) {
			throw new PersistenceException( "unexpected error when checking transaction status", e );
		}
	}

}
