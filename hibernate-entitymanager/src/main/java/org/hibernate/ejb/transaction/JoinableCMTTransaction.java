/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.transaction;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.internal.jta.CMTTransaction;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;

/**
 * Implements a joinable transaction. Until the transaction is marked for joined, the TM.isTransactionInProgress()
 * must return false
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JoinableCMTTransaction extends CMTTransaction {
	private JoinStatus joinStatus = JoinStatus.NOT_JOINED;

	public JoinableCMTTransaction(TransactionCoordinator transactionCoordinator) {
		super( transactionCoordinator );
	}

	boolean isJoinable() {
		return joinStatus == JoinStatus.JOINED && JtaStatusHelper.isActive( transactionManager() );
	}

	public JoinStatus getJoinStatus() {
		return joinStatus;
	}

	@Override
	public void join() {
		if ( joinStatus == JoinStatus.MARKED_FOR_JOINED ) {
			if ( JtaStatusHelper.isActive( transactionManager() ) ) {
				joinStatus = JoinStatus.JOINED;
				// register synchronization if needed
				transactionCoordinator().pulse();
			}
			else {
				joinStatus = JoinStatus.NOT_JOINED;
			}
		}
	}

	@Override
	public void resetJoinStatus() {
		joinStatus = JoinStatus.NOT_JOINED;
	}

	@Override
	public void begin() throws HibernateException {
		super.begin();
		joinStatus = JoinStatus.JOINED;
	}

	@Override
	public void commit() throws HibernateException {
		/* this method is not supposed to be called
		 * it breaks the flushBeforeCompletion flag optimization
		 * regarding flushing skip.
		 * In its current form, it will generate too much flush() calls
		 */
		super.commit();
	}


	public JoinStatus getStatus() {
		return joinStatus;
	}

	public void resetStatus() {
		joinStatus = JoinStatus.NOT_JOINED;
	}

}
