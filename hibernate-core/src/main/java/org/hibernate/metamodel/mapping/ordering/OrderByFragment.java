/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering;

import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * Represents the translation result.  Defines the ability to apply the indicated ordering to the SQL AST
 * being built
 *
 * @author Steve Ebersole
 */
public interface OrderByFragment {
	/**
	 * Apply the ordering to the given SQL AST
	 *
	 * @param ast The SQL AST
	 * @param tableGroup The TableGroup the order-by is applied "against"
	 * @param creationState The SQL AST creation state
	 */
	void apply(QuerySpec ast, TableGroup tableGroup, SqlAstCreationState creationState);

}
