/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;

/// Read-only semantic facts supplied by Hibernate when selecting how to render
/// a table join.
///
/// The request deliberately omits translator state. Implementations of
/// [TableJoinRenderingSupport] should select a plan from these facts and must
/// not retain the request.
///
/// @since 8.0
/// @author Steve Ebersole
public interface TableJoinRenderingRequest {
	/// The structural kind of joined table reference.
	TableJoinKind kind();

	/// The semantic SQL AST join type.
	SqlAstJoinType joinType();

	/// Whether the joined table reference is lateral.
	boolean lateral();

	/// Whether the join occurs while rendering a recursive query part.
	boolean recursiveQueryPart();

	/// Whether an explicit join predicate is present.
	boolean predicatePresent();
}
