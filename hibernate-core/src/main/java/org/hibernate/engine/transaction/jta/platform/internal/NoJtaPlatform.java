/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

/**
 * The non-configured form of JTA platform.  This is what is used if none was set up.
 *
 * @author Steve Ebersole
 */
public class NoJtaPlatform implements JtaPlatform {
	public static final NoJtaPlatform INSTANCE = new NoJtaPlatform();

	@Override
	public TransactionManager retrieveTransactionManager() {
		return null;
	}

	@Override
	public UserTransaction retrieveUserTransaction() {
		return null;
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		return null;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
	}

	@Override
	public boolean canRegisterSynchronization() {
		return false;
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return Status.STATUS_UNKNOWN;
	}
}
