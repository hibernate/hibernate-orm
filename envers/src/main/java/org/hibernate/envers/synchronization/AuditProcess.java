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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.hibernate.action.BeforeTransactionCompletionProcess;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.envers.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.tools.Pair;

import org.hibernate.FlushMode;
import org.hibernate.Session;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditProcess implements BeforeTransactionCompletionProcess {
    private final RevisionInfoGenerator revisionInfoGenerator;

    private final LinkedList<AuditWorkUnit> workUnits;
    private final Queue<AuditWorkUnit> undoQueue;
    private final Map<Pair<String, Object>, AuditWorkUnit> usedIds;

    private Object revisionData;

    public AuditProcess(RevisionInfoGenerator revisionInfoGenerator) {
        this.revisionInfoGenerator = revisionInfoGenerator;

        workUnits = new LinkedList<AuditWorkUnit>();
        undoQueue = new LinkedList<AuditWorkUnit>();
        usedIds = new HashMap<Pair<String, Object>, AuditWorkUnit>();
    }

    private void removeWorkUnit(AuditWorkUnit vwu) {
        workUnits.remove(vwu);
        if (vwu.isPerformed()) {
            // If this work unit has already been performed, it must be deleted (undone) first.
            undoQueue.offer(vwu);
        }
    }

    public void addWorkUnit(AuditWorkUnit vwu) {
        if (vwu.containsWork()) {
            Object entityId = vwu.getEntityId();

            if (entityId == null) {
                // Just adding the work unit - it's not associated with any persistent entity.
                workUnits.offer(vwu);
            } else {
                String entityName = vwu.getEntityName();
                Pair<String, Object> usedIdsKey = Pair.make(entityName, entityId);

                if (usedIds.containsKey(usedIdsKey)) {
                    AuditWorkUnit other = usedIds.get(usedIdsKey);

                    AuditWorkUnit result = vwu.dispatch(other);

                    if (result != other) {
                        removeWorkUnit(other);

                        if (result != null) {
                            usedIds.put(usedIdsKey, result);
                            workUnits.offer(result);
                        } // else: a null result means that no work unit should be kept
                    } // else: the result is the same as the work unit already added. No need to do anything.
                } else {
                    usedIds.put(usedIdsKey, vwu);
                    workUnits.offer(vwu);
                }
            }
        }
    }

    private void executeInSession(Session session) {
		// Making sure the revision data is persisted.
        getCurrentRevisionData(session, true);

        AuditWorkUnit vwu;

        // First undoing any performed work units
        while ((vwu = undoQueue.poll()) != null) {
            vwu.undo(session);
        }

        while ((vwu = workUnits.poll()) != null) {
            vwu.perform(session, revisionData);
        }
    }

	public Object getCurrentRevisionData(Session session, boolean persist) {
		// Generating the revision data if not yet generated
		if (revisionData == null) {
            revisionData = revisionInfoGenerator.generate();
        }

		// Saving the revision data, if not yet saved and persist is true
		if (!session.contains(revisionData) && persist) {
			revisionInfoGenerator.saveRevisionData(session, revisionData);
		}

		return revisionData;
	}

    public void doBeforeTransactionCompletion(SessionImplementor session) {
        if (workUnits.size() == 0 && undoQueue.size() == 0) {
            return;
        }

        // see: http://www.jboss.com/index.html?module=bb&op=viewtopic&p=4178431
        if (FlushMode.isManualFlushMode(session.getFlushMode())) {
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
            executeInSession((Session) session);

            // Explicity flushing the session, as the auto-flush may have already happened.
            session.flush();
        }
    }
}
