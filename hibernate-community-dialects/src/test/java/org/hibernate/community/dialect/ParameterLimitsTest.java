/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that community Dialects preserve their existing parameter limits
/// when supplying the unified profile.
///
/// @author Steve Ebersole
public class ParameterLimitsTest {
	@Test
	void communityDialectsPreserveTheirLimits() {
		assertThat( new AltibaseDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 1_000 ) );
		assertThat( new DB2LegacyDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 32_767 ) );
		assertThat( new DerbyDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 512 ) );
		assertThat( new FirebirdDialect( DatabaseVersion.make( 4 ) ).getParameterLimits() )
				.isEqualTo( ParameterLimits.of( 1_500 ) );
		assertThat( new FirebirdDialect( DatabaseVersion.make( 5 ) ).getParameterLimits() )
				.isEqualTo( ParameterLimits.of( 65_535 ) );
		assertThat( new InterSystemsIRISDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 900 ) );
		assertThat( new OracleLegacyDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 1_000 ) );
		assertThat( new SQLServerLegacyDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 2_100 ) );
		assertThat( new SQLiteDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 1_000 ) );
		assertThat( new SingleStoreDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 1_048_576 ) );
		assertThat( new SybaseLegacyDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 250_000 ) );
		assertThat( new TeradataDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 1_024 ) );
	}
}
