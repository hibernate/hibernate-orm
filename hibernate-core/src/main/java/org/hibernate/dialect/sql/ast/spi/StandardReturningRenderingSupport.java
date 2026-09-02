/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

/// Standard and family returning-column rendering profiles.
///
/// @since 8.0
/// @author Steve Ebersole
public final class StandardReturningRenderingSupport {
	private static final ReturningRenderingPlan.Clause CLAUSE = new ReturningRenderingPlan.Clause();
	private static final ReturningRenderingPlan.None NONE = new ReturningRenderingPlan.None();
	private static final ReturningRenderingPlan.ChangeTable OLD_TABLE = changeTable( ChangeTableKind.OLD );
	private static final ReturningRenderingPlan.ChangeTable NEW_TABLE = changeTable( ChangeTableKind.NEW );
	private static final ReturningRenderingPlan.ChangeTable FINAL_TABLE = changeTable( ChangeTableKind.FINAL );

	/// Standard trailing `returning` syntax.
	public static final ReturningRenderingSupport STANDARD = request ->
			isEmpty( request ) ? NONE : CLAUSE;

	/// DB2 LUW and IBM i data-change table syntax.
	public static final ReturningRenderingSupport DB2 = db2( NEW_TABLE );

	/// DB2 for z/OS data-change table syntax.
	public static final ReturningRenderingSupport DB2_ZOS = db2( FINAL_TABLE );

	/// H2 data-change table syntax for model-driven inserts and updates.
	public static final ReturningRenderingSupport H2 = request -> {
		if ( isEmpty( request ) ) {
			return NONE;
		}
		if ( request.source() == ReturningMutationSource.MODEL
				&& request.mutationKind() != MutationKind.DELETE ) {
			return FINAL_TABLE;
		}
		throw new UnsupportedOperationException(
				"H2 does not support returning columns for " + request.source() + " "
						+ request.mutationKind() + " mutations"
		);
	};

	private StandardReturningRenderingSupport() {
	}

	private static ReturningRenderingSupport db2(ReturningRenderingPlan.ChangeTable newTablePlan) {
		return request -> {
			if ( isEmpty( request ) ) {
				return NONE;
			}
			return switch ( request.mutationKind() ) {
				case DELETE -> OLD_TABLE;
				case INSERT -> newTablePlan;
				case UPDATE -> request.source() == ReturningMutationSource.MODEL ? FINAL_TABLE : newTablePlan;
			};
		};
	}

	private static boolean isEmpty(ReturningRenderingRequest request) {
		Objects.requireNonNull( request, "request" );
		Objects.requireNonNull( request.mutationKind(), "request.mutationKind" );
		Objects.requireNonNull( request.source(), "request.source" );
		return Objects.requireNonNull( request.returningColumns(), "request.returningColumns" ).isEmpty();
	}

	private static ReturningRenderingPlan.ChangeTable changeTable(ChangeTableKind kind) {
		return new ReturningRenderingPlan.ChangeTable( kind );
	}
}
