/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.event.spi;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.ModWorkUnit;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Envers-specific entity (post) update event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversPostUpdateEventListenerImpl extends BaseEnversUpdateEventListener implements PostUpdateEventListener {
	public EnversPostUpdateEventListenerImpl(EnversService enversService) {
		super( enversService );
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		final String entityName = event.getPersister().getEntityName();

		if ( getEnversService().getEntitiesConfigurations().isVersioned( entityName ) ) {
			checkIfTransactionInProgress( event.getSession() );

			final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get( event.getSession() );

			Object[] oldState = getOldDBState( auditProcess, entityName, event );
			final Object[] newDbState = postUpdateDBState( event );
			final AuditWorkUnit workUnit = new ModWorkUnit(
					event.getSession(),
					event.getPersister().getEntityName(),
					getEnversService(),
					event.getId(),
					event.getPersister(),
					newDbState,
					oldState
			);
			auditProcess.addWorkUnit( workUnit );

			if ( workUnit.containsWork() ) {
				generateBidirectionalCollectionChangeWorkUnits(
						auditProcess,
						event.getPersister(),
						entityName,
						newDbState,
						oldState,
						event.getSession()
				);
			}
		}
	}

	private Object[] getOldDBState(AuditProcess auditProcess, String entityName, PostUpdateEvent event) {
		if ( isDetachedEntityUpdate( entityName, event.getOldState() ) ) {
			return auditProcess.getCachedEntityState( event.getId(), entityName );
		}
		return event.getOldState();
	}

	private Object[] postUpdateDBState(PostUpdateEvent event) {
		final Object[] newDbState = event.getState().clone();
		if ( event.getOldState() != null ) {
			final EntityPersister entityPersister = event.getPersister();
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

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return getEnversService().getEntitiesConfigurations().isVersioned( persister.getEntityName() );
	}
}
