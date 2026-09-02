/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hibernate.dialect.sql.ast.spi.ValuesListSupport.Context.INSERT;
import static org.hibernate.dialect.sql.ast.spi.ValuesListSupport.Context.QUERY;

/// Tests the immutable, context-specific values-list capability profile.
///
/// @since 8.0
/// @author Steve Ebersole
public class ValuesListSupportTests {
	@Test
	void contextsAreIndependentAndImmutable() {
		assertThat( ValuesListSupport.NONE.getContexts() ).isEmpty();
		assertThat( ValuesListSupport.INSERT_ONLY.supports( INSERT ) ).isTrue();
		assertThat( ValuesListSupport.INSERT_ONLY.supports( QUERY ) ).isFalse();
		assertThat( ValuesListSupport.STANDARD.getContexts() ).containsExactlyInAnyOrder( QUERY, INSERT );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> ValuesListSupport.STANDARD.getContexts().remove( QUERY ) );
	}

	@Test
	void dialectProfilesRetainVersionAndFamilyDifferences() {
		assertThat( new PostgreSQLDialect().getValuesListSupport().getContexts() )
				.containsExactlyInAnyOrder( QUERY, INSERT );
		assertThat( new HANADialect().getValuesListSupport().getContexts() ).isEmpty();
		assertThat( new OracleDialect( DatabaseVersion.make( 22 ) ).getValuesListSupport().getContexts() ).isEmpty();
		assertThat( new OracleDialect( DatabaseVersion.make( 23 ) ).getValuesListSupport().getContexts() )
				.containsExactlyInAnyOrder( QUERY, INSERT );
	}
}
