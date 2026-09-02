/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;

/// Standard and family table-join rendering strategies.
///
/// @since 8.0
/// @author Steve Ebersole
public final class StandardTableJoinRenderingSupport {
	private static final TableJoinRenderingPlan STANDARD_PLAN = new TableJoinRenderingPlan.Standard();
	private static final TableJoinRenderingPlan COMMA_PLAN = new TableJoinRenderingPlan.Comma();
	private static final TableJoinRenderingPlan CROSS_APPLY_PLAN =
			new TableJoinRenderingPlan.Apply( TableJoinRenderingPlan.Apply.Kind.CROSS );
	private static final TableJoinRenderingPlan OUTER_APPLY_PLAN =
			new TableJoinRenderingPlan.Apply( TableJoinRenderingPlan.Apply.Kind.OUTER );
	private static final TableJoinRenderingPlan UNSUPPORTED_PLAN = new TableJoinRenderingPlan.Unsupported();

	/// Standard SQL join rendering.
	public static final TableJoinRenderingSupport STANDARD = request -> {
		Objects.requireNonNull( request, "request" );
		return STANDARD_PLAN;
	};

	/// DB2-family recursive-query join rendering.
	public static final TableJoinRenderingSupport DB2 = request -> {
		Objects.requireNonNull( request, "request" );
		if ( !request.recursiveQueryPart() ) {
			return STANDARD_PLAN;
		}
		return switch ( request.joinType() ) {
			case INNER, CROSS -> COMMA_PLAN;
			default -> UNSUPPORTED_PLAN;
		};
	};

	/// SQL Server-family lateral join rendering.
	public static final TableJoinRenderingSupport SQL_SERVER = request -> {
		Objects.requireNonNull( request, "request" );
		if ( request.kind() != TableJoinKind.TABLE_GROUP || !request.lateral() ) {
			return STANDARD_PLAN;
		}
		return request.joinType() == SqlAstJoinType.LEFT ? OUTER_APPLY_PLAN : CROSS_APPLY_PLAN;
	};

	private StandardTableJoinRenderingSupport() {
	}
}
