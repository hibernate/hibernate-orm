/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.FetchClauseType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.query.common.FetchClauseType.PERCENT_ONLY;
import static org.hibernate.query.common.FetchClauseType.ROWS_ONLY;
import static org.hibernate.query.common.FetchClauseType.ROWS_WITH_TIES;

/// Verifies immutable fetch-clause profiles and maintained supply points.
///
/// @author Steve Ebersole
public class FetchClauseSupportTest {
	@Test
	void stockProfilesContainExactlyTheirDocumentedForms() {
		assertThat( FetchClauseSupport.NONE.getSupportedTypes() ).isEmpty();
		assertThat( FetchClauseSupport.ROWS_ONLY.getSupportedTypes() ).containsExactly( ROWS_ONLY );
		assertThat( FetchClauseSupport.ROWS.getSupportedTypes() )
				.containsExactlyInAnyOrder( ROWS_ONLY, ROWS_WITH_TIES );
		assertThat( FetchClauseSupport.ALL.getSupportedTypes() )
				.containsExactlyInAnyOrder( FetchClauseType.values() );
	}

	@Test
	void adHocProfilesAreIndependentImmutableAndCanonicalized() {
		final var asymmetric = FetchClauseSupport.of( ROWS_ONLY, PERCENT_ONLY, ROWS_ONLY );
		assertThat( asymmetric.getSupportedTypes() ).containsExactlyInAnyOrder( ROWS_ONLY, PERCENT_ONLY );
		assertThat( asymmetric.supports( ROWS_WITH_TIES ) ).isFalse();
		assertThatThrownBy( () -> asymmetric.getSupportedTypes().add( ROWS_WITH_TIES ) )
				.isInstanceOf( UnsupportedOperationException.class );

		assertThat( FetchClauseSupport.of() ).isSameAs( FetchClauseSupport.NONE );
		assertThat( FetchClauseSupport.of( ROWS_ONLY ) ).isSameAs( FetchClauseSupport.ROWS_ONLY );
		assertThat( FetchClauseSupport.of( ROWS_WITH_TIES, ROWS_ONLY ) ).isSameAs( FetchClauseSupport.ROWS );
		assertThat( FetchClauseSupport.of( FetchClauseType.values() ) ).isSameAs( FetchClauseSupport.ALL );
	}

	@Test
	void nullInputsAreRejected() {
		assertThatIllegalArgumentException().isThrownBy( () -> FetchClauseSupport.of( (FetchClauseType[]) null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> FetchClauseSupport.of( ROWS_ONLY, null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> FetchClauseSupport.NONE.supports( null ) );
	}

	@Test
	void maintainedProfilesAreExact() {
		assertThat( new Dialect( DatabaseVersion.make( 1 ) ) {}.getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.NONE );
		assertThat( new H2Dialect().getFetchClauseSupport() ).isSameAs( FetchClauseSupport.ALL );
		assertThat( new PostgreSQLDialect().getFetchClauseSupport() ).isSameAs( FetchClauseSupport.ROWS );
		assertThat( new SpannerPostgreSQLDialect().getFetchClauseSupport() ).isSameAs( FetchClauseSupport.NONE );
	}
}
