/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
}
