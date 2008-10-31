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

import org.hibernate.envers.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.tools.ConcurrentReferenceHashMap;

import org.hibernate.Transaction;
import org.hibernate.event.EventSource;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditSyncManager {
    private final Map<Transaction, AuditSync> versionsSyncs;
    private final RevisionInfoGenerator revisionInfoGenerator;

    public AuditSyncManager(RevisionInfoGenerator revisionInfoGenerator) {
        versionsSyncs = new ConcurrentReferenceHashMap<Transaction, AuditSync>(10,
                ConcurrentReferenceHashMap.ReferenceType.WEAK,
                ConcurrentReferenceHashMap.ReferenceType.STRONG);

        this.revisionInfoGenerator = revisionInfoGenerator;
    }

    public AuditSync get(EventSource session) {
        Transaction transaction = session.getTransaction();

        AuditSync verSync = versionsSyncs.get(transaction);
        if (verSync == null) {
            verSync = new AuditSync(this, session, revisionInfoGenerator);
            versionsSyncs.put(transaction, verSync);

            transaction.registerSynchronization(verSync);
        }

        return verSync;
    }

    public void remove(Transaction transaction) {
        versionsSyncs.remove(transaction);
    }
}
