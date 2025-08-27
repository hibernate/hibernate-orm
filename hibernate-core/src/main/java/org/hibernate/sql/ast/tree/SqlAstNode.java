/*
 * SPDX-License-Identifier: Apache-2.0
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
