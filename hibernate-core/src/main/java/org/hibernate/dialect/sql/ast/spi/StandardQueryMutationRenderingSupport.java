/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

/// Standard immutable query-mutation rendering profiles.
///
/// [#STANDARD] honors native mutation capabilities. The named family profiles
/// select their configured update form whenever a nontrivial from-clause is
/// present, allowing a family to own nonstandard native or emulation grammar.
///
/// @since 8.0
/// @author Steve Ebersole
public final class StandardQueryMutationRenderingSupport implements QueryMutationRenderingSupport {
	/// Use native capabilities and the generic scalar-subquery or join fallbacks.
	public static final QueryMutationRenderingSupport STANDARD =
			new StandardQueryMutationRenderingSupport(
					new UpdateRenderingPlan.ScalarSubquery(),
					new DeleteRenderingPlan.JoinEmulation(),
					true
			);

	/// Use merge for updates containing a nontrivial from-clause.
	public static final QueryMutationRenderingSupport MERGE =
			withNonTrivialUpdatePlan( new UpdateRenderingPlan.Merge() );

	/// Use an inline view for updates containing a nontrivial from-clause.
	public static final QueryMutationRenderingSupport INLINE_VIEW =
			withNonTrivialUpdatePlan( new UpdateRenderingPlan.InlineView() );

	/// Use tuple assignment for updates containing a nontrivial from-clause.
	public static final QueryMutationRenderingSupport TUPLE_SET =
			withNonTrivialUpdatePlan( new UpdateRenderingPlan.TupleSet() );

	private final UpdateRenderingPlan nonTrivialUpdatePlan;
	private final DeleteRenderingPlan nonNativeDeletePlan;
	private final boolean honorNativeUpdateSyntax;

	private StandardQueryMutationRenderingSupport(
			UpdateRenderingPlan nonTrivialUpdatePlan,
			DeleteRenderingPlan nonNativeDeletePlan,
			boolean honorNativeUpdateSyntax) {
		this.nonTrivialUpdatePlan = Objects.requireNonNull( nonTrivialUpdatePlan, "nonTrivialUpdatePlan" );
		this.nonNativeDeletePlan = Objects.requireNonNull( nonNativeDeletePlan, "nonNativeDeletePlan" );
		this.honorNativeUpdateSyntax = honorNativeUpdateSyntax;
	}

	/// Create a profile with the given nontrivial update plan and standard delete emulation.
	public static QueryMutationRenderingSupport withNonTrivialUpdatePlan(UpdateRenderingPlan updatePlan) {
		return new StandardQueryMutationRenderingSupport(
				updatePlan,
				new DeleteRenderingPlan.JoinEmulation(),
				false
		);
	}

	/// Create a profile with the given update emulation and target-aliased delete emulation.
	public static QueryMutationRenderingSupport withTargetAliasedDelete(
			UpdateRenderingPlan updatePlan,
			String targetAlias) {
		return new StandardQueryMutationRenderingSupport(
				updatePlan,
				new DeleteRenderingPlan.JoinEmulation( Objects.requireNonNull( targetAlias, "targetAlias" ) ),
				false
		);
	}

	/// Create a profile using the same target alias for correlated scalar-update
	/// assignments and delete join emulation.
	public static QueryMutationRenderingSupport withTargetAliasedScalarSubquery(String targetAlias) {
		final String requiredAlias = Objects.requireNonNull( targetAlias, "targetAlias" );
		return new StandardQueryMutationRenderingSupport(
				new UpdateRenderingPlan.ScalarSubquery( requiredAlias ),
				new DeleteRenderingPlan.JoinEmulation( requiredAlias ),
				false
		);
	}

	@Override
	public UpdateRenderingPlan determineUpdatePlan(UpdateRenderingRequest request) {
		Objects.requireNonNull( request, "request" );
		return !request.hasNonTrivialFromClause()
				|| honorNativeUpdateSyntax && request.mutationSyntaxSupport().supports(
						MutationKind.UPDATE,
						MutationSyntaxCapability.FROM_CLAUSE
				)
				? new UpdateRenderingPlan.Direct()
				: nonTrivialUpdatePlan;
	}

	@Override
	public DeleteRenderingPlan determineDeletePlan(DeleteRenderingRequest request) {
		Objects.requireNonNull( request, "request" );
		return !request.hasNonTrivialFromClause()
				|| request.mutationSyntaxSupport().supports(
						MutationKind.DELETE,
						MutationSyntaxCapability.JOIN
				)
				? new DeleteRenderingPlan.Direct()
				: nonNativeDeletePlan;
	}
}
