/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.sql.ast.spi.DB2QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.DeleteRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.DeleteRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.UpdateRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.UpdateRenderingRequest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.sql.ast.spi.MutationKind.DELETE;
import static org.hibernate.dialect.sql.ast.spi.MutationKind.UPDATE;
import static org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability.FROM_CLAUSE;
import static org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability.JOIN;

/// Tests contextual query-mutation plan selection independently of rendering.
///
/// @author Steve Ebersole
public class QueryMutationRenderingSupportTests {
	@Test
	void standardSupportUsesNativeSyntaxWhenAvailable() {
		final QueryMutationRenderingSupport support = StandardQueryMutationRenderingSupport.STANDARD;
		final MutationSyntaxSupport syntax = MutationSyntaxSupport.builder()
				.capability( UPDATE, FROM_CLAUSE )
				.capability( DELETE, JOIN )
				.build();

		assertThat( support.determineUpdatePlan( updateRequest( true, false, syntax ) ) )
				.isInstanceOf( UpdateRenderingPlan.Direct.class );
		assertThat( support.determineDeletePlan( deleteRequest( true, syntax ) ) )
				.isInstanceOf( DeleteRenderingPlan.Direct.class );
	}

	@Test
	void standardSupportUsesDefaultEmulationsWhenNativeSyntaxIsUnavailable() {
		final QueryMutationRenderingSupport support = StandardQueryMutationRenderingSupport.STANDARD;

		assertThat( support.determineUpdatePlan( updateRequest( true, false, MutationSyntaxSupport.NONE ) ) )
				.isEqualTo( new UpdateRenderingPlan.ScalarSubquery() );
		assertThat( support.determineDeletePlan( deleteRequest( true, MutationSyntaxSupport.NONE ) ) )
				.isEqualTo( new DeleteRenderingPlan.JoinEmulation() );
	}

	@Test
	void familySupportSelectsItsEmulationForANontrivialFromClause() {
		final MutationSyntaxSupport nativeFrom = MutationSyntaxSupport.of( UPDATE, FROM_CLAUSE );

		assertThat( StandardQueryMutationRenderingSupport.INLINE_VIEW.determineUpdatePlan(
				updateRequest( true, false, nativeFrom )
		) ).isInstanceOf( UpdateRenderingPlan.InlineView.class );
	}

	@Test
	void targetAliasedDeleteSupportCarriesOnlyTheAlias() {
		final QueryMutationRenderingSupport support =
				StandardQueryMutationRenderingSupport.withTargetAliasedDelete(
						new UpdateRenderingPlan.Merge(),
						"dml_target_"
				);

		assertThat( support.determineDeletePlan( deleteRequest( true, MutationSyntaxSupport.NONE ) ) )
				.isEqualTo( new DeleteRenderingPlan.JoinEmulation( "dml_target_" ) );
	}

	@Test
	void targetAliasedScalarSubquerySupportCarriesTheAliasToBothMutationPlans() {
		final QueryMutationRenderingSupport support =
				StandardQueryMutationRenderingSupport.withTargetAliasedScalarSubquery( "dml_target_" );

		assertThat( support.determineUpdatePlan(
				updateRequest( true, false, MutationSyntaxSupport.NONE )
		) ).isEqualTo( new UpdateRenderingPlan.ScalarSubquery( "dml_target_" ) );
		assertThat( support.determineDeletePlan( deleteRequest( true, MutationSyntaxSupport.NONE ) ) )
				.isEqualTo( new DeleteRenderingPlan.JoinEmulation( "dml_target_" ) );
	}

	@Test
	void db2UsesTupleSetOnlyForReturningWrappedEmulation() {
		final QueryMutationRenderingSupport support = DB2QueryMutationRenderingSupport.INSTANCE;

		assertThat( support.determineUpdatePlan(
				updateRequest( true, false, MutationSyntaxSupport.NONE )
		) ).isInstanceOf( UpdateRenderingPlan.Merge.class );
		assertThat( support.determineUpdatePlan(
				updateRequest( true, true, MutationSyntaxSupport.NONE )
		) ).isInstanceOf( UpdateRenderingPlan.TupleSet.class );
		assertThat( support.determineUpdatePlan(
				updateRequest( true, true, MutationSyntaxSupport.of( UPDATE, FROM_CLAUSE ) )
		) ).isInstanceOf( UpdateRenderingPlan.Direct.class );
	}

	private static UpdateRenderingRequest updateRequest(
			boolean nonTrivialFromClause,
			boolean returningColumns,
			MutationSyntaxSupport mutationSyntaxSupport) {
		return new UpdateRequest( nonTrivialFromClause, returningColumns, mutationSyntaxSupport );
	}

	private static DeleteRenderingRequest deleteRequest(
			boolean nonTrivialFromClause,
			MutationSyntaxSupport mutationSyntaxSupport) {
		return new DeleteRequest( nonTrivialFromClause, false, mutationSyntaxSupport );
	}

	private record UpdateRequest(
			boolean hasNonTrivialFromClause,
			boolean hasReturningColumns,
			MutationSyntaxSupport mutationSyntaxSupport) implements UpdateRenderingRequest {
	}

	private record DeleteRequest(
			boolean hasNonTrivialFromClause,
			boolean hasReturningColumns,
			MutationSyntaxSupport mutationSyntaxSupport) implements DeleteRenderingRequest {
	}
}
