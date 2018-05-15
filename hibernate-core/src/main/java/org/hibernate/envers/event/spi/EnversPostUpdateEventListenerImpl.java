/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.event.spi;

import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.ModWorkUnit;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Envers-specific entity (post) update event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversPostUpdateEventListenerImpl extends BaseEnversUpdateEventListener implements PostUpdateEventListener {
	public EnversPostUpdateEventListenerImpl(AuditService auditService) {
		super( auditService );
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		final String entityName = event.getDescriptor().getEntityName();

		if ( getAuditService().getEntityBindings().isVersioned( entityName ) ) {
			checkIfTransactionInProgress( event.getSession() );

			final AuditProcess auditProcess = getAuditService().getAuditProcess( event.getSession() );

			Object[] oldState = getOldDBState( auditProcess, entityName, event );
			final Object[] newDbState = postUpdateDBState( event );
			final AuditWorkUnit workUnit = new ModWorkUnit(
					event.getSession(),
					event.getDescriptor().getEntityName(),
					getAuditService(),
					event.getId(),
					event.getDescriptor(),
					newDbState,
					oldState
			);
			auditProcess.addWorkUnit( workUnit );

			if ( workUnit.containsWork() ) {
				generateBidirectionalCollectionChangeWorkUnits(
						auditProcess,
						event.getDescriptor(),
						entityName,
						newDbState,
						event.getOldState(),
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
			final EntityTypeDescriptor<?> entityDescriptor = event.getDescriptor();
			entityDescriptor.visitStateArrayContributors(
					contributor -> {
						int index = contributor.getStateArrayPosition();
						if ( !contributor.isUpdatable() ) {
							// Assuming that PostUpdateEvent#getOldState() returns database state of the record before
							// modification. Otherwise, we would have to execute SQL query to be sure of
							// @Column(updatable = false) column value.
							newDbState[ index ] = event.getOldState()[ index ];
						}
					}
			);
		}
		return newDbState;
	}

	@Override
	public boolean requiresPostCommitHandling(EntityTypeDescriptor descriptor) {
		return getAuditService().getEntityBindings().isVersioned( descriptor.getEntityName() );
	}
}
