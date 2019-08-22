/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.event.spi.EventSource;

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

	public AuditProcess get(EventSource session) {
		final Transaction transaction = session.accessTransaction();

		AuditProcess auditProcess = auditProcesses.get( transaction );
		if ( auditProcess == null ) {
			// No worries about registering a transaction twice - a transaction is single thread
			auditProcess = new AuditProcess( revisionInfoGenerator, session );
			auditProcesses.put( transaction, auditProcess );

			session.getActionQueue().registerProcess((SessionImplementor session1) -> {
				final AuditProcess process = auditProcesses.get( transaction );
				if (process != null) {
					process.doBeforeTransactionCompletion(session1);
				}
			});

			session.getActionQueue().registerProcess((boolean success, SharedSessionContractImplementor session1) -> {
				auditProcesses.remove( transaction );
			});
		}

		return auditProcess;
	}
}
