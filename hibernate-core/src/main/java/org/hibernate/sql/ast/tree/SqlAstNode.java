/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree;

import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public interface SqlAstNode {
	void accept(SqlAstWalker sqlTreeWalker);
}
