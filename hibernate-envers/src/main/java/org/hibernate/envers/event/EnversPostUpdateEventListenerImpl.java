/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.event;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.synchronization.AuditProcess;
import org.hibernate.envers.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.synchronization.work.ModWorkUnit;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 */
public class EnversPostUpdateEventListenerImpl extends BaseEnversEventListener implements PostUpdateEventListener {
	protected EnversPostUpdateEventListenerImpl(AuditConfiguration enversConfiguration) {
		super( enversConfiguration );
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
        String entityName = event.getPersister().getEntityName();

        if ( getAuditConfiguration().getEntCfg().isVersioned(entityName) ) {
            checkIfTransactionInProgress(event.getSession());

            AuditProcess auditProcess = getAuditConfiguration().getSyncManager().get(event.getSession());

			final Object[] newDbState = postUpdateDBState( event );

            AuditWorkUnit workUnit = new ModWorkUnit(
					event.getSession(),
					event.getPersister().getEntityName(),
					getAuditConfiguration(),
                    event.getId(),
					event.getPersister(),
					newDbState,
					event.getOldState()
			);
            auditProcess.addWorkUnit( workUnit );

            if ( workUnit.containsWork() ) {
                generateBidirectionalCollectionChangeWorkUnits(
						auditProcess,
						event.getPersister(),
						entityName,
						newDbState,
                        event.getOldState(),
						event.getSession()
				);
            }
        }
	}

	private Object[] postUpdateDBState(PostUpdateEvent event) {
		Object[] newDbState = event.getState().clone();
		if ( event.getOldState() != null ) {
			EntityPersister entityPersister = event.getPersister();
			for ( int i = 0; i < entityPersister.getPropertyNames().length; ++i ) {
				if ( !entityPersister.getPropertyUpdateability()[i] ) {
					// Assuming that PostUpdateEvent#getOldState() returns database state of the record before modification.
					// Otherwise, we would have to execute SQL query to be sure of @Column(updatable = false) column value.
					newDbState[i] = event.getOldState()[i];
				}
			}
		}
		return newDbState;
	}
}
