/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import org.hibernate.query.IllegalQueryOperationException;

/// Standard immutable insert-conflict rendering profiles.
///
/// @since 8.0
/// @author Steve Ebersole
public final class StandardInsertConflictRenderingSupport {
	/// Emulate supported do-nothing requests through constraint-violation handling.
	public static final InsertConflictRenderingSupport CONSTRAINT_VIOLATION = request -> switch ( action( request ) ) {
		case NONE -> new InsertConflictRenderingPlan.None();
		case DO_NOTHING -> new InsertConflictRenderingPlan.ConstraintViolation();
		case DO_UPDATE -> throw unsupported( "Insert conflict do update clause is not supported" );
	};

	/// Render standard `on conflict` syntax.
	public static final InsertConflictRenderingSupport STANDARD = request ->
			action( request ) == InsertConflictAction.NONE
					? new InsertConflictRenderingPlan.None()
					: new InsertConflictRenderingPlan.Standard();

	/// Render do-update requests as merge and emulate do-nothing through constraint handling.
	public static final InsertConflictRenderingSupport MERGE = merge( false );

	/// Render do-update requests as a semicolon-terminated merge statement.
	public static final InsertConflictRenderingSupport TERMINATED_MERGE = merge( true );

	/// Render legacy MySQL-family proposed-row references with `values(column)`.
	public static final InsertConflictRenderingSupport ON_DUPLICATE_KEY_VALUES_FUNCTION = onDuplicateKey(
			DoNothingSyntax.SELF_ASSIGNMENT,
			ValuesRowReferenceStyle.VALUES_FUNCTION
	);

	/// Render MySQL-family proposed-row references through an explicit row alias.
	public static final InsertConflictRenderingSupport ON_DUPLICATE_KEY_ROW_ALIAS = onDuplicateKey(
			DoNothingSyntax.SELF_ASSIGNMENT,
			ValuesRowReferenceStyle.ROW_ALIAS
	);

	/// Render GaussDB's `nothing` action and implicit `excluded` pseudo-row.
	public static final InsertConflictRenderingSupport ON_DUPLICATE_KEY_NOTHING = onDuplicateKey(
			DoNothingSyntax.NOTHING_KEYWORD,
			ValuesRowReferenceStyle.IMPLICIT_EXCLUDED
	);

	private StandardInsertConflictRenderingSupport() {
	}

	/// Create a merge profile with optional statement termination.
	public static InsertConflictRenderingSupport merge(boolean terminateStatement) {
		return request -> switch ( action( request ) ) {
			case NONE -> new InsertConflictRenderingPlan.None();
			case DO_NOTHING -> new InsertConflictRenderingPlan.ConstraintViolation();
			case DO_UPDATE -> new InsertConflictRenderingPlan.Merge( terminateStatement );
		};
	}

	/// Create an on-duplicate-key profile.
	public static InsertConflictRenderingSupport onDuplicateKey(
			DoNothingSyntax doNothingSyntax,
			ValuesRowReferenceStyle valuesRowReferenceStyle) {
		final InsertConflictRenderingPlan plan = new InsertConflictRenderingPlan.OnDuplicateKey(
				doNothingSyntax,
				valuesRowReferenceStyle
		);
		return request -> {
			final InsertConflictAction action = action( request );
			if ( action == InsertConflictAction.NONE ) {
				return new InsertConflictRenderingPlan.None();
			}
			if ( request.constraintName() != null ) {
				if ( action == InsertConflictAction.DO_UPDATE ) {
					throw unsupported( "Insert conflict 'do update' clause with constraint name is not supported" );
				}
				return new InsertConflictRenderingPlan.ConstraintViolation();
			}
			return plan;
		};
	}

	private static InsertConflictAction action(InsertConflictRenderingRequest request) {
		return Objects.requireNonNull( request, "request" ).action();
	}

	private static IllegalQueryOperationException unsupported(String message) {
		return new IllegalQueryOperationException( message );
	}
}
