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
package org.hibernate.envers.event.spi;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
	protected EnversPostDeleteEventListenerImpl(AuditConfiguration enversConfiguration) {
		super( enversConfiguration );
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		final String entityName = event.getPersister().getEntityName();

		if ( getAuditConfiguration().getEntCfg().isVersioned( entityName ) ) {
			checkIfTransactionInProgress( event.getSession() );

			final AuditProcess auditProcess = getAuditConfiguration().getSyncManager().get( event.getSession() );

			final AuditWorkUnit workUnit = new DelWorkUnit(
					event.getSession(),
					event.getPersister().getEntityName(),
					getAuditConfiguration(),
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
		return getAuditConfiguration().getEntCfg().isVersioned( persister.getEntityName() );
	}
}
