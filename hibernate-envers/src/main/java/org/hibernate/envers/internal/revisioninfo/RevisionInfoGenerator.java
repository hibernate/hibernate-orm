/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import org.hibernate.Session;
import org.hibernate.envers.RevisionType;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface RevisionInfoGenerator {
	/**
	 * Set the revision entity number reader instance.
	 */
	void setRevisionInfoNumberReader(RevisionInfoNumberReader revisionInfoNumberReader);

	void saveRevisionData(Session session, Object revisionData);

	Object generate();

	/**
	 * @see org.hibernate.envers.EntityTrackingRevisionListener#entityChanged(Class, String, Object, RevisionType, Object)
	 */
	void entityChanged(
			Class entityClass, String entityName, Object entityId, RevisionType revisionType,
			Object revisionEntity);
}
