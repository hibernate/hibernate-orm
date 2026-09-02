/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import java.util.List;

import org.hibernate.dialect.sql.ast.spi.ChangeTableKind;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.ReturningMutationSource;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardReturningRenderingSupport;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/// Tests the standard returning-column rendering profiles exposed to Dialect
/// providers.
///
/// @since 8.0
/// @author Steve Ebersole
public class ReturningRenderingSupportTests {
	private static final List<ColumnReference> RETURNING_COLUMNS = List.of( mock( ColumnReference.class ) );

	@Test
	void emptyRequestsAlwaysSelectNone() {
		for ( ReturningRenderingSupport support : List.of(
				StandardReturningRenderingSupport.STANDARD,
				StandardReturningRenderingSupport.DB2,
				StandardReturningRenderingSupport.DB2_ZOS,
				StandardReturningRenderingSupport.H2 ) ) {
			assertThat( plan( support, MutationKind.INSERT, ReturningMutationSource.QUERY, List.of() ) )
					.isEqualTo( new ReturningRenderingPlan.None() );
		}
	}

	@Test
	void standardProfileSelectsTrailingClause() {
		assertThat( plan(
				StandardReturningRenderingSupport.STANDARD,
				MutationKind.UPDATE,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isEqualTo( new ReturningRenderingPlan.Clause() );
	}

	@Test
	void everyGeneratedValuesReturningProfileHasViableModelMutationPlans() {
		assertThat( plan(
				StandardReturningRenderingSupport.STANDARD,
				MutationKind.INSERT,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
		assertThat( plan(
				StandardReturningRenderingSupport.STANDARD,
				MutationKind.UPDATE,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
		assertThat( plan(
				StandardReturningRenderingSupport.DB2,
				MutationKind.INSERT,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
		assertThat( plan(
				StandardReturningRenderingSupport.DB2,
				MutationKind.UPDATE,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
		assertThat( plan(
				StandardReturningRenderingSupport.DB2_ZOS,
				MutationKind.INSERT,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
		assertThat( plan(
				StandardReturningRenderingSupport.DB2_ZOS,
				MutationKind.UPDATE,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
		assertThat( plan(
				StandardReturningRenderingSupport.H2,
				MutationKind.INSERT,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
		assertThat( plan(
				StandardReturningRenderingSupport.H2,
				MutationKind.UPDATE,
				ReturningMutationSource.MODEL,
				RETURNING_COLUMNS
		) ).isNotInstanceOf( ReturningRenderingPlan.None.class );
	}

	@Test
	void db2ProfileAccountsForMutationKindAndSemanticSource() {
		assertChangeTable(
				StandardReturningRenderingSupport.DB2,
				MutationKind.DELETE,
				ReturningMutationSource.QUERY,
				ChangeTableKind.OLD
		);
		assertChangeTable(
				StandardReturningRenderingSupport.DB2,
				MutationKind.INSERT,
				ReturningMutationSource.MODEL,
				ChangeTableKind.NEW
		);
		assertChangeTable(
				StandardReturningRenderingSupport.DB2,
				MutationKind.UPDATE,
				ReturningMutationSource.QUERY,
				ChangeTableKind.NEW
		);
		assertChangeTable(
				StandardReturningRenderingSupport.DB2,
				MutationKind.UPDATE,
				ReturningMutationSource.MODEL,
				ChangeTableKind.FINAL
		);
	}

	@Test
	void db2ZosUsesFinalInsteadOfNewImages() {
		assertChangeTable(
				StandardReturningRenderingSupport.DB2_ZOS,
				MutationKind.INSERT,
				ReturningMutationSource.QUERY,
				ChangeTableKind.FINAL
		);
		assertChangeTable(
				StandardReturningRenderingSupport.DB2_ZOS,
				MutationKind.UPDATE,
				ReturningMutationSource.QUERY,
				ChangeTableKind.FINAL
		);
	}

	@Test
	void h2UsesFinalTablesForSupportedModelMutationsAndRejectsQueryReturning() {
		assertChangeTable(
				StandardReturningRenderingSupport.H2,
				MutationKind.INSERT,
				ReturningMutationSource.MODEL,
				ChangeTableKind.FINAL
		);
		assertChangeTable(
				StandardReturningRenderingSupport.H2,
				MutationKind.UPDATE,
				ReturningMutationSource.MODEL,
				ChangeTableKind.FINAL
		);
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> plan(
						StandardReturningRenderingSupport.H2,
						MutationKind.DELETE,
						ReturningMutationSource.QUERY,
						RETURNING_COLUMNS
				) )
				.withMessageContaining( "QUERY DELETE" );
	}

	private static void assertChangeTable(
			ReturningRenderingSupport support,
			MutationKind mutationKind,
			ReturningMutationSource source,
			ChangeTableKind expectedKind) {
		assertThat( plan( support, mutationKind, source, RETURNING_COLUMNS ) )
				.isEqualTo( new ReturningRenderingPlan.ChangeTable( expectedKind ) );
	}

	private static ReturningRenderingPlan plan(
			ReturningRenderingSupport support,
			MutationKind mutationKind,
			ReturningMutationSource source,
			List<ColumnReference> returningColumns) {
		return support.determinePlan( new Request( mutationKind, source, returningColumns ) );
	}

	private record Request(
			MutationKind mutationKind,
			ReturningMutationSource source,
			List<ColumnReference> returningColumns) implements ReturningRenderingRequest {
	}
}
