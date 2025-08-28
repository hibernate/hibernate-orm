/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.synchronization.work;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.RevisionType;

/**
 * TODO: refactor constructors into factory methods
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditWorkUnit extends WorkUnitMergeVisitor, WorkUnitMergeDispatcher {
	Object getEntityId();

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
