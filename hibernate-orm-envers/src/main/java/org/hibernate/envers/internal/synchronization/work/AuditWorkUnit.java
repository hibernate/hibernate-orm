/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.RevisionType;

/**
 * TODO: refactor constructors into factory methods
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditWorkUnit extends WorkUnitMergeVisitor, WorkUnitMergeDispatcher {
	Serializable getEntityId();

	String getEntityName();

	boolean containsWork();

	boolean isPerformed();

	/**
	 * Perform this work unit in the given session.
	 *
	 * @param session Session, in which the work unit should be performed.
	 * @param revisionData The current revision data, which will be used to populate the work unit with the correct
	 * revision relation.
	 */
	void perform(Session session, Object revisionData);

	void undo(Session session);

	/**
	 * @param revisionData The current revision data, which will be used to populate the work unit with the correct
	 * revision relation.
	 *
	 * @return Generates data that should be saved when performing this work unit.
	 */
	Map<String, Object> generateData(Object revisionData);

	/**
	 * @return Performed modification type.
	 */
	RevisionType getRevisionType();
}
