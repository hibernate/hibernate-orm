/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import org.hibernate.dialect.sql.ast.spi.DoNothingSyntax;
import org.hibernate.dialect.sql.ast.spi.InsertConflictAction;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesRowReferenceStyle;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/// Tests standard insert-conflict rendering profiles exposed to Dialect
/// providers.
///
/// @since 8.0
/// @author Steve Ebersole
public class InsertConflictRenderingSupportTests {
	@Test
	void noConflictAlwaysSelectsNone() {
		for ( InsertConflictRenderingSupport support : new InsertConflictRenderingSupport[] {
				StandardInsertConflictRenderingSupport.CONSTRAINT_VIOLATION,
				StandardInsertConflictRenderingSupport.STANDARD,
				StandardInsertConflictRenderingSupport.MERGE,
				StandardInsertConflictRenderingSupport.TERMINATED_MERGE,
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION,
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_ROW_ALIAS,
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_NOTHING
		} ) {
			assertThat( plan( support, InsertConflictAction.NONE, null, false, null ) )
					.isEqualTo( new InsertConflictRenderingPlan.None() );
		}
	}

	@Test
	void standardAndMergeProfilesSelectSemanticPlans() {
		assertThat( plan(
				StandardInsertConflictRenderingSupport.STANDARD,
				InsertConflictAction.DO_UPDATE,
				null,
				true,
				null
		) ).isEqualTo( new InsertConflictRenderingPlan.Standard() );
		assertThat( plan(
				StandardInsertConflictRenderingSupport.MERGE,
				InsertConflictAction.DO_NOTHING,
				null,
				false,
				null
		) ).isEqualTo( new InsertConflictRenderingPlan.ConstraintViolation() );
		assertThat( plan(
				StandardInsertConflictRenderingSupport.TERMINATED_MERGE,
				InsertConflictAction.DO_UPDATE,
				null,
				true,
				null
		) ).isEqualTo( new InsertConflictRenderingPlan.Merge( true ) );
	}

	@Test
	void onDuplicateProfilesCaptureDoNothingAndProposedRowSyntax() {
		assertThat( plan(
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION,
				InsertConflictAction.DO_UPDATE,
				null,
				true,
				null
		) ).isEqualTo( new InsertConflictRenderingPlan.OnDuplicateKey(
				DoNothingSyntax.SELF_ASSIGNMENT,
				ValuesRowReferenceStyle.VALUES_FUNCTION
		) );
		assertThat( plan(
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_ROW_ALIAS,
				InsertConflictAction.DO_NOTHING,
				null,
				true,
				null
		) ).isEqualTo( new InsertConflictRenderingPlan.OnDuplicateKey(
				DoNothingSyntax.SELF_ASSIGNMENT,
				ValuesRowReferenceStyle.ROW_ALIAS
		) );
		assertThat( plan(
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_NOTHING,
				InsertConflictAction.DO_NOTHING,
				null,
				true,
				null
		) ).isEqualTo( new InsertConflictRenderingPlan.OnDuplicateKey(
				DoNothingSyntax.NOTHING_KEYWORD,
				ValuesRowReferenceStyle.IMPLICIT_EXCLUDED
		) );
	}

	@Test
	void namedDoNothingUsesConstraintFailureAndNamedDoUpdateIsRejected() {
		assertThat( plan(
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION,
				InsertConflictAction.DO_NOTHING,
				"uk_example",
				false,
				null
		) ).isEqualTo( new InsertConflictRenderingPlan.ConstraintViolation() );
		assertThatExceptionOfType( RuntimeException.class )
				.isThrownBy( () -> plan(
						StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION,
						InsertConflictAction.DO_UPDATE,
						"uk_example",
						false,
						null
				) )
				.withMessageContaining( "constraint name" );
	}

	private static InsertConflictRenderingPlan plan(
			InsertConflictRenderingSupport support,
			InsertConflictAction action,
			String constraintName,
			boolean hasConstraintColumns,
			Predicate predicate) {
		return support.determinePlan( new Request( action, constraintName, hasConstraintColumns, predicate ) );
	}

	private record Request(
			InsertConflictAction action,
			String constraintName,
			boolean hasConstraintColumns,
			Predicate predicate) implements InsertConflictRenderingRequest {
	}
}
