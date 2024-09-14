/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.cte;

/**
 * The kind of materialization that should be used for a CTE.
 * This is a hint that like e.g. for PostgreSQL which allows to control this.
 *
 * @author Christian Beikov
 */
public enum CteMaterialization {
	/**
	 * It is undefined if the CTE should be materialized.
	 */
	UNDEFINED,
	/**
	 * Materialize the CTE if possible.
	 */
	MATERIALIZED,
	/**
	 * Do not materialize the CTE if possible.
	 */
	NOT_MATERIALIZED;
}
