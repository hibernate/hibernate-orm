/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
