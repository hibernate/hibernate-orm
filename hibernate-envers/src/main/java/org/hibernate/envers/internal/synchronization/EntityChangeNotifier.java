/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.synchronization;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.PersistentCollectionChangeWorkUnit;
import org.hibernate.envers.internal.tools.EntityTools;

/**
 * Notifies {@link RevisionInfoGenerator} about changes made in the current revision.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EntityChangeNotifier {
	private final RevisionInfoGenerator revisionInfoGenerator;
	private final SessionImplementor sessionImplementor;

	public EntityChangeNotifier(RevisionInfoGenerator revisionInfoGenerator, SessionImplementor sessionImplementor) {
		this.revisionInfoGenerator = revisionInfoGenerator;
		this.sessionImplementor = sessionImplementor;
	}

	/**
	 * Notifies {@link RevisionInfoGenerator} about changes made in the current revision. Provides information
	 * about modified entity class, entity name and its id, as well as {@link org.hibernate.envers.RevisionType}
	 * and revision log entity.
	 *
	 * @param session Active session.
	 * @param currentRevisionData Revision log entity.
	 * @param vwu Performed work unit.
	 */
	public void entityChanged(Session session, Object currentRevisionData, AuditWorkUnit vwu) {
		Serializable entityId = vwu.getEntityId();
		if ( entityId instanceof PersistentCollectionChangeWorkUnit.PersistentCollectionChangeWorkUnitId ) {
			// Notify about a change in collection owner entity.
			entityId = ((PersistentCollectionChangeWorkUnit.PersistentCollectionChangeWorkUnitId) entityId).getOwnerId();
		}
		final Class entityClass = EntityTools.getEntityClass( sessionImplementor, session, vwu.getEntityName() );
		revisionInfoGenerator.entityChanged(
				entityClass,
				vwu.getEntityName(),
				entityId,
				vwu.getRevisionType(),
				currentRevisionData
		);
	}
}
