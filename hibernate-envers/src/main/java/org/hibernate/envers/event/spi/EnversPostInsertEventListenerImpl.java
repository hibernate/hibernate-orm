/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.event.spi;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.work.AddWorkUnit;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Envers-specific entity (post) insertion event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 */
public class EnversPostInsertEventListenerImpl extends BaseEnversEventListener implements PostInsertEventListener {
	public EnversPostInsertEventListenerImpl(EnversService enversService) {
		super( enversService );
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		final String entityName = event.getPersister().getEntityName();

		if ( getEnversService().getEntitiesConfigurations().isVersioned( entityName ) ) {
			checkIfTransactionInProgress( event.getSession() );

			final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get( event.getSession() );

			final AuditWorkUnit workUnit = new AddWorkUnit(
					event.getSession(),
					event.getPersister().getEntityName(),
					getEnversService(),
					event.getId(),
					event.getPersister(),
					event.getState()
			);
			auditProcess.addWorkUnit( workUnit );

			if ( workUnit.containsWork() ) {
				generateBidirectionalCollectionChangeWorkUnits(
						auditProcess,
						event.getPersister(),
						entityName,
						event.getState(),
						null,
						event.getSession()
				);
			}
		}
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return getEnversService().getEntitiesConfigurations().isVersioned( persister.getEntityName() );
	}
}
