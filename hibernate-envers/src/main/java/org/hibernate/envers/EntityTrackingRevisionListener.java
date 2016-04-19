/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers;

import java.io.Serializable;

/**
 * Extension of standard {@link RevisionListener} that notifies whenever an entity instance has been
 * added, modified or removed within current revision boundaries.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @see RevisionListener
 */
public interface EntityTrackingRevisionListener extends RevisionListener {
	/**
	 * Called afterQuery audited entity data has been persisted.
	 *
	 * @param entityClass Audited entity class.
	 * @param entityName Name of the audited entity. May be useful when Java class is mapped multiple times,
	 * potentially to different tables.
	 * @param entityId Identifier of modified entity.
	 * @param revisionType Modification type (addition, update or removal).
	 * @param revisionEntity An instance of the entity annotated with {@link RevisionEntity}.
	 */
	void entityChanged(
			Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
			Object revisionEntity);
}
