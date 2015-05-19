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
import org.hibernate.envers.internal.synchronization.work.DelWorkUnit;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Envers-specific entity (post) deletion event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 */
public class EnversPostDeleteEventListenerImpl extends BaseEnversEventListener implements PostDeleteEventListener {
	public EnversPostDeleteEventListenerImpl(EnversService enversService) {
		super( enversService );
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		final String entityName = event.getPersister().getEntityName();

		if ( getEnversService().getEntitiesConfigurations().isVersioned( entityName ) ) {
			checkIfTransactionInProgress( event.getSession() );

			final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get( event.getSession() );

			final AuditWorkUnit workUnit = new DelWorkUnit(
					event.getSession(),
					event.getPersister().getEntityName(),
					getEnversService(),
					event.getId(),
					event.getPersister(),
					event.getDeletedState()
			);
			auditProcess.addWorkUnit( workUnit );

			if ( workUnit.containsWork() ) {
				generateBidirectionalCollectionChangeWorkUnits(
						auditProcess,
						event.getPersister(),
						entityName,
						null,
						event.getDeletedState(),
						event.getSession()
				);
			}
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return getEnversService().getEntitiesConfigurations().isVersioned( persister.getEntityName() );
	}
}
