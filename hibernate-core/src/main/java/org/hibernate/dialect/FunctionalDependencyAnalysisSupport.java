/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Dialect support information for primary key functional dependency analysis
 * within {@code GROUP BY} and {@code ORDER BY} clauses.
 *
 * @author Marco Belladelli
 */
public interface FunctionalDependencyAnalysisSupport {
	/**
	 * Supports primary key functional dependency analysis
	 */
	boolean supportsAnalysis();

	/**
	 * Supports functional dependency analysis through joined tables and result sets (e.g. unions)
	 */
	boolean supportsTableGroups();

	/**
	 * Also supports functional dependency analysis for constant values other than table columns
	 */
	boolean supportsConstants();
}
