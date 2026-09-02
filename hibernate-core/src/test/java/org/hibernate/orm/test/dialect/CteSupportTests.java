/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.sql.ast.spi.CteSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.MutationFeature.INSERT_CONFLICT;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.MutationFeature.NON_QUERY;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.Placement.NESTED;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.Placement.NONE;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.Placement.SUBQUERY;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.Placement.TOP_LEVEL;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.RecursiveFeature.CYCLE;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.RecursiveFeature.CYCLE_USING;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.RecursiveFeature.RECURSIVE;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.RecursiveFeature.SEARCH;

/// Tests the hierarchy and prerequisite rules of [CteSupport].
///
/// @author Steve Ebersole
public class CteSupportTests {
	@Test
	void placementIsHierarchical() {
		final CteSupport topLevel = CteSupport.builder().placement( TOP_LEVEL ).build();
		final CteSupport subquery = CteSupport.builder().placement( SUBQUERY ).build();
		final CteSupport nested = CteSupport.builder().placement( NESTED ).build();

		assertThat( CteSupport.NONE.supportsWithClause() ).isFalse();
		assertThat( topLevel.supportsWithClause() ).isTrue();
		assertThat( topLevel.supportsWithClauseInSubquery() ).isFalse();
		assertThat( subquery.supportsWithClauseInSubquery() ).isTrue();
		assertThat( subquery.supportsNestedWithClause() ).isFalse();
		assertThat( nested.supportsNestedWithClause() ).isTrue();
	}

	@Test
	void copiedProfilesAreIndependent() {
		final CteSupport base = CteSupport.builder()
				.placement( SUBQUERY )
				.recursiveFeatures( RECURSIVE, SEARCH )
				.mutationFeatures( NON_QUERY )
				.build();
		final CteSupport copy = CteSupport.builder( base )
				.placement( NESTED )
				.recursiveFeatures( RECURSIVE, CYCLE )
				.mutationFeatures( NON_QUERY, INSERT_CONFLICT )
				.build();

		assertThat( base.getPlacement() ).isEqualTo( SUBQUERY );
		assertThat( base.getRecursiveFeatures() ).containsExactlyInAnyOrder( RECURSIVE, SEARCH );
		assertThat( base.getMutationFeatures() ).containsExactly( NON_QUERY );
		assertThat( copy.getPlacement() ).isEqualTo( NESTED );
		assertThat( copy.getRecursiveFeatures() ).containsExactlyInAnyOrder( RECURSIVE, CYCLE );
		assertThat( copy.getMutationFeatures() ).containsExactlyInAnyOrder( NON_QUERY, INSERT_CONFLICT );
	}

	@Test
	void conditionalFeatureSelectionCanAddAndRemoveCopiedFeatures() {
		final CteSupport base = CteSupport.builder()
				.recursiveFeatures( RECURSIVE, SEARCH )
				.build();
		final CteSupport copy = CteSupport.builder( base )
				.recursiveFeature( SEARCH, false )
				.recursiveFeature( CYCLE, true )
				.build();

		assertThat( base.getRecursiveFeatures() ).containsExactlyInAnyOrder( RECURSIVE, SEARCH );
		assertThat( copy.getRecursiveFeatures() ).containsExactlyInAnyOrder( RECURSIVE, CYCLE );
	}

	@Test
	void featuresRequireWithClauseSupport() {
		assertThatIllegalArgumentException().isThrownBy( () -> CteSupport.builder()
				.placement( NONE )
				.recursiveFeatures( RECURSIVE )
				.build() );
		assertThatIllegalArgumentException().isThrownBy( () -> CteSupport.builder()
				.placement( NONE )
				.mutationFeatures( NON_QUERY )
				.build() );
	}

	@Test
	void recursiveRefinementsRequireTheirParents() {
		assertThatIllegalArgumentException().isThrownBy( () -> CteSupport.builder()
				.recursiveFeatures( SEARCH )
				.build() );
		assertThatIllegalArgumentException().isThrownBy( () -> CteSupport.builder()
				.recursiveFeatures( RECURSIVE, CYCLE_USING )
				.build() );
	}

	@Test
	void insertConflictRequiresNonQueryCteSupport() {
		assertThatIllegalArgumentException().isThrownBy( () -> CteSupport.builder()
				.mutationFeatures( INSERT_CONFLICT )
				.build() );
	}
}
