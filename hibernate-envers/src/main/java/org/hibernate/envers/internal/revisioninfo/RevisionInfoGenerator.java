/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.envers.RevisionType;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface RevisionInfoGenerator {
	void saveRevisionData(Session session, Object revisionData);

	Object generate();

	/**
	 * @see org.hibernate.envers.EntityTrackingRevisionListener#entityChanged(Class, String, Serializable, RevisionType, Object)
	 */
	void entityChanged(
			Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
			Object revisionEntity);
}
