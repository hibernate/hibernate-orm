/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.synchronization.work;


/**
 * Visitor patter dispatcher.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface WorkUnitMergeDispatcher {
	/**
	 * Should be invoked on the second work unit.
	 *
	 * @param first First work unit (that is, the one added earlier).
	 *
	 * @return The work unit that is the result of the merge.
	 */
	AuditWorkUnit dispatch(WorkUnitMergeVisitor first);
}
