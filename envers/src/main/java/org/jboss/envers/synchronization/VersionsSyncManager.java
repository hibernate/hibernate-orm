/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.synchronization;

import org.jboss.envers.tools.ConcurrentReferenceHashMap;
import org.jboss.envers.revisioninfo.RevisionInfoGenerator;
import org.hibernate.Transaction;
import org.hibernate.event.EventSource;

import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsSyncManager {
    private final Map<Transaction, VersionsSync> versionsSyncs;
    private final RevisionInfoGenerator revisionInfoGenerator;

    public VersionsSyncManager(RevisionInfoGenerator revisionInfoGenerator) {
        versionsSyncs = new ConcurrentReferenceHashMap<Transaction, VersionsSync>(10,
                ConcurrentReferenceHashMap.ReferenceType.WEAK,
                ConcurrentReferenceHashMap.ReferenceType.STRONG);

        this.revisionInfoGenerator = revisionInfoGenerator;
    }

    public VersionsSync get(EventSource session) {
        Transaction transaction = session.getTransaction();

        VersionsSync verSync = versionsSyncs.get(transaction);
        if (verSync == null) {
            verSync = new VersionsSync(this, session, revisionInfoGenerator);
            versionsSyncs.put(transaction, verSync);

            transaction.registerSynchronization(verSync);
        }

        return verSync;
    }

    public void remove(Transaction transaction) {
        versionsSyncs.remove(transaction);
    }
}
