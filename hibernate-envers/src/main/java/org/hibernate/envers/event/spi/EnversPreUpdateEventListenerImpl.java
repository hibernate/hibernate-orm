/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.event.spi;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

/**
 * Envers-specific entity (pre) update event listener.
 *
 * @author Chris Cranford
 */
public class EnversPreUpdateEventListenerImpl extends BaseEnversUpdateEventListener implements PreUpdateEventListener {
	public EnversPreUpdateEventListenerImpl(EnversService enversService) {
		super( enversService );
	}

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		final String entityName = event.getPersister().getEntityName();
		if ( getEnversService().getEntitiesConfigurations().isVersioned( entityName ) ) {
			checkIfTransactionInProgress( event.getSession() );
			if ( isDetachedEntityUpdate( entityName, event.getOldState() ) ) {
				final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get( event.getSession() );
				auditProcess.cacheEntityState(
						event.getId(),
						entityName,
						event.getPersister().getDatabaseSnapshot( event.getId(), event.getSession() )
				);
			}
		}
		return false;
	}
}
