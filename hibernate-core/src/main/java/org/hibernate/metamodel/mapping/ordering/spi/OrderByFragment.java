/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.spi;

import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;

/// Applies a mapped collection ordering to a SQL AST.
///
/// Providers implementing this contract should apply only the ordering modeled
/// by this fragment and should not retain the supplied creation state.
///
/// @author Steve Ebersole
/// @since 8.0
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
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
