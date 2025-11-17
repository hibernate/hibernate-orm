/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree;

import org.hibernate.sql.ast.SqlAstWalker;

/**
 * Base contract for any statement
 *
 * @author Steve Ebersole
 */
public interface Statement {
	/**
	 * Visitation
	 */
	void accept(SqlAstWalker walker);

	/**
	 * Whether this statement is a selection and will return results.
	 */
	default boolean isSelection() {
		return false;
	}
}
