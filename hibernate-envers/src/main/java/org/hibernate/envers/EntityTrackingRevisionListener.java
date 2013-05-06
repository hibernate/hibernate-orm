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
	 * Called after audited entity data has been persisted.
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
