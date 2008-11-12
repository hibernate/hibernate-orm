/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.envers.synchronization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.envers.revisioninfo.RevisionInfoGenerator;

import org.hibernate.Transaction;
import org.hibernate.event.EventSource;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditSyncManager {
    private final Map<Transaction, AuditSync> auditSyncs;
    private final RevisionInfoGenerator revisionInfoGenerator;

    public AuditSyncManager(RevisionInfoGenerator revisionInfoGenerator) {
        auditSyncs = new ConcurrentHashMap<Transaction, AuditSync>();

        this.revisionInfoGenerator = revisionInfoGenerator;
    }

    public AuditSync get(EventSource session) {
        Transaction transaction = session.getTransaction();

        AuditSync verSync = auditSyncs.get(transaction);
        if (verSync == null) {
            // No worries about registering a transaction twice - a transaction is single thread
            verSync = new AuditSync(this, session, revisionInfoGenerator);
            auditSyncs.put(transaction, verSync);

            transaction.registerSynchronization(verSync);
        }

        return verSync;
    }

    public void remove(Transaction transaction) {
        auditSyncs.remove(transaction);
    }
}
