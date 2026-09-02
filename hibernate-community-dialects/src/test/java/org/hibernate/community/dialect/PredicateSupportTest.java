/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies every community Dialect predicate profile, including all
/// version-dependent branches.
///
/// @author Steve Ebersole
public class PredicateSupportTest {
	@Test
	void communityDialectsPreserveEveryPredicateDimension() {
		assertProfile(
				new DB2LegacyDialect( DatabaseVersion.make( 10, 5 ) ).getPredicateSupport(),
				null
		);
		assertProfile(
				new DB2LegacyDialect( DatabaseVersion.make( 11 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new DB2LegacyDialect( DatabaseVersion.make( 11, 1 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new DB2iLegacyDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM
		);
		assertProfile(
				new DB2zLegacyDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM
		);
		assertProfile(
				new HSQLLegacyDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SQLServerLegacyDialect( DatabaseVersion.make( 15 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SQLServerLegacyDialect( DatabaseVersion.make( 16 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new GaussDBDialect().getPredicateSupport(),
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new CUBRIDDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new FirebirdDialect( DatabaseVersion.make( 2, 5 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM
		);
		assertProfile(
				new FirebirdDialect( DatabaseVersion.make( 3 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SingleStoreDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new H2LegacyDialect( DatabaseVersion.make( 1, 4, 193 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new H2LegacyDialect( DatabaseVersion.make( 1, 4, 194 ) ).getPredicateSupport(),
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new PostgreSQLLegacyDialect().getPredicateSupport(),
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile( new AltibaseDialect().getPredicateSupport(), null );
		assertProfile(
				new CockroachLegacyDialect().getPredicateSupport(),
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SybaseASELegacyDialect( DatabaseVersion.make( 16, 2 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SybaseASELegacyDialect( DatabaseVersion.make( 16, 3 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new MySQLLegacyDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
	}

	private static void assertProfile(
			PredicateSupport profile,
			String operator,
			PredicateSupport.Capability... capabilities) {
		if ( operator == null ) {
			assertThat( profile.getCaseInsensitiveLikeOperator() ).isEmpty();
		}
		else {
			assertThat( profile.getCaseInsensitiveLikeOperator() ).contains( operator );
		}
		assertThat( profile.getCapabilities() ).containsExactlyInAnyOrder( capabilities );
	}
}
