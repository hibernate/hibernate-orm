/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies every community functional-dependency supply point.
///
/// @author Steve Ebersole
public class FunctionalDependencyAnalysisSupportTest {
	@Test
	void communityProfilesPreserveTheirEffectiveValues() {
		assertThat( new HSQLLegacyDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_REFERENCE );
		assertThat( new TiDBDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_REFERENCE );
		assertThat( new PostgreSQLLegacyDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_REFERENCE );
		assertThat( new MariaDBLegacyDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_GROUP_AND_CONSTANTS );
		assertThat( new SingleStoreDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_GROUP );
		assertThat( new H2LegacyDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_GROUP_AND_CONSTANTS );
		assertThat( new GaussDBDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_REFERENCE );
		assertThat( new CockroachLegacyDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_REFERENCE );
		assertThat( new MySQLLegacyDialect().getFunctionalDependencyAnalysisSupport() )
				.isEqualTo( FunctionalDependencyAnalysisSupport.TABLE_GROUP );
	}
}
