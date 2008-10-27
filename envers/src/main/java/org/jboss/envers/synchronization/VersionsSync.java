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
package org.jboss.envers.synchronization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import javax.transaction.Synchronization;

import org.jboss.envers.revisioninfo.RevisionInfoGenerator;
import org.jboss.envers.synchronization.work.VersionsWorkUnit;
import org.jboss.envers.tools.Pair;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.event.EventSource;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsSync implements Synchronization {
    private final RevisionInfoGenerator revisionInfoGenerator;
    private final VersionsSyncManager manager;
    private final EventSource session;

    private final Transaction transaction;
    private final LinkedList<VersionsWorkUnit> workUnits;
    private final Queue<VersionsWorkUnit> undoQueue;
    private final Map<Pair<String, Object>, VersionsWorkUnit> usedIds;

    private Object revisionData;

    public VersionsSync(VersionsSyncManager manager, EventSource session, RevisionInfoGenerator revisionInfoGenerator) {
        this.manager = manager;
        this.session = session;
        this.revisionInfoGenerator = revisionInfoGenerator;

        transaction = session.getTransaction();
        workUnits = new LinkedList<VersionsWorkUnit>();
        undoQueue = new LinkedList<VersionsWorkUnit>();
        usedIds = new HashMap<Pair<String, Object>, VersionsWorkUnit>();
    }

    private void removeWorkUnit(VersionsWorkUnit vwu) {
        workUnits.remove(vwu);
        if (vwu.isPerformed()) {
            // If this work unit has already been performed, it must be deleted (undone) first.
            undoQueue.offer(vwu);
        }
    }

    public void addWorkUnit(VersionsWorkUnit vwu) {
        if (vwu.containsWork()) {
            Object entityId = vwu.getEntityId();

            if (entityId == null) {
                // Just adding the work unit - it's not associated with any persistent entity.
                workUnits.offer(vwu);
            } else {
                String entityName = vwu.getEntityName();
                Pair<String, Object> usedIdsKey = Pair.make(entityName, entityId);

                if (usedIds.containsKey(usedIdsKey)) {
                    VersionsWorkUnit other = usedIds.get(usedIdsKey);

                    // The entity with entityId has two work units; checking which one should be kept.
                    switch (vwu.dispatch(other)) {
                        case FIRST:
                            // Simply not adding the second
                            break;

                        case SECOND:
                            removeWorkUnit(other);
                            usedIds.put(usedIdsKey, vwu);
                            workUnits.offer(vwu);
                            break;

                        case NONE:
                            removeWorkUnit(other);
                            break;
                    }
                } else {
                    usedIds.put(usedIdsKey, vwu);
                    workUnits.offer(vwu);
                }
            }
        }
    }

    private void executeInSession(Session session) {
        if (revisionData == null) {
            revisionData = revisionInfoGenerator.generate(session);
        }

        VersionsWorkUnit vwu;

        // First undoing any performed work units
        while ((vwu = undoQueue.poll()) != null) {
            vwu.undo(session);
        }

        while ((vwu = workUnits.poll()) != null) {
            vwu.perform(session, revisionData);
        }
    }

    public void beforeCompletion() {
        if (workUnits.size() == 0 && undoQueue.size() == 0) {
            return;
        }

        // see: http://www.jboss.com/index.html?module=bb&op=viewtopic&p=4178431
        if (FlushMode.isManualFlushMode(session.getFlushMode()) || session.isClosed()) {
            Session temporarySession = null;
            try {
                temporarySession = session.getFactory().openTemporarySession();

                executeInSession(temporarySession);

                temporarySession.flush();
            } finally {
                if (temporarySession != null) {
                    temporarySession.close();
                }
            }
        } else {
            executeInSession(session);

            // Explicity flushing the session, as the auto-flush may have already happened.
            session.flush();
        }
    }

    public void afterCompletion(int i) {
        manager.remove(transaction);
    }
}
