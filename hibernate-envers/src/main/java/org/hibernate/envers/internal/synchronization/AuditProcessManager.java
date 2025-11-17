/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.synchronization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditProcessManager {
	private final Map<Transaction, AuditProcess> auditProcesses;
	private final RevisionInfoGenerator revisionInfoGenerator;

	public AuditProcessManager(RevisionInfoGenerator revisionInfoGenerator) {
		auditProcesses = new ConcurrentHashMap<>();

		this.revisionInfoGenerator = revisionInfoGenerator;
	}

	public AuditProcess get(SharedSessionContractImplementor session) {
		final Transaction transaction = session.accessTransaction();

		AuditProcess auditProcess = auditProcesses.get( transaction );
		if ( auditProcess == null ) {
			// No worries about registering a transaction twice - a transaction is single thread
			auditProcess = new AuditProcess( revisionInfoGenerator, session );
			auditProcesses.put( transaction, auditProcess );

			final TransactionCompletionCallbacks transactionCompletionCallbacks = session.getTransactionCompletionCallbacks();
			transactionCompletionCallbacks.registerCallback( new BeforeTransactionCompletionProcess() {
				public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
					final AuditProcess process = auditProcesses.get( transaction );
					if ( process != null ) {
						process.doBeforeTransactionCompletion( session );
					}
				}
			} );

			transactionCompletionCallbacks.registerCallback( new AfterTransactionCompletionProcess() {
				public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
					auditProcesses.remove( transaction );
				}
			} );
		}

		return auditProcess;
	}
}
