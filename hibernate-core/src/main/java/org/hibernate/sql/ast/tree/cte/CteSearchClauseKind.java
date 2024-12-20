/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

/**
 * The kind of CTE search clause.
 *
 * @author Christian Beikov
 */
public enum CteSearchClauseKind {
	/**
	 * Use depth first for a recursive CTE.
	 */
	DEPTH_FIRST,
	/**
	 * Use breadth first for a recursive CTE.
	 */
	BREADTH_FIRST;
}
