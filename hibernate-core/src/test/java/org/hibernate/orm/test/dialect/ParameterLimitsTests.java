/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the immutable JDBC parameter-limit profile and its use by standard
/// batch sizing.
///
/// @author Steve Ebersole
public class ParameterLimitsTests {
	@Test
	void unlimitedProfileNormalizesNonpositiveValues() {
		assertThat( ParameterLimits.UNLIMITED.inExpressionCountLimit() ).isZero();
		assertThat( ParameterLimits.UNLIMITED.parameterCountLimit() ).isZero();
		assertThat( ParameterLimits.of( 0 ) ).isSameAs( ParameterLimits.UNLIMITED );
		assertThat( ParameterLimits.of( -1 ) ).isSameAs( ParameterLimits.UNLIMITED );
		assertThat( new ParameterLimits( -5, 0 ) ).isEqualTo( ParameterLimits.UNLIMITED );
	}

	@Test
	void sharedLimitAppliesToBothDimensions() {
		final ParameterLimits limits = ParameterLimits.of( 128 );
		assertThat( limits.inExpressionCountLimit() ).isEqualTo( 128 );
		assertThat( limits.parameterCountLimit() ).isEqualTo( 128 );
	}

	@Test
	void dimensionsMayVaryIndependently() {
		final ParameterLimits limits = new ParameterLimits( 250_000, 2_000 );
		assertThat( limits.inExpressionCountLimit() ).isEqualTo( 250_000 );
		assertThat( limits.parameterCountLimit() ).isEqualTo( 2_000 );

		assertThat( new SybaseDialect().getParameterLimits() ).isEqualTo( limits );
	}

	@Test
	void maintainedDialectsPreserveTheirLimits() {
		assertThat( new DB2Dialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 32_767 ) );
		assertThat( new OracleDialect( DatabaseVersion.make( 22 ) ).getParameterLimits() )
				.isEqualTo( ParameterLimits.of( 1_000 ) );
		assertThat( new OracleDialect( DatabaseVersion.make( 23 ) ).getParameterLimits() )
				.isEqualTo( ParameterLimits.of( 65_535 ) );
		assertThat( new SQLServerDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 2_048 ) );
		assertThat( new SpannerPostgreSQLDialect().getParameterLimits() ).isEqualTo( ParameterLimits.of( 100 ) );
	}

	@Test
	void standardBatchSizingUsesTheStatementParameterLimit() {
		final SybaseDialect dialect = new SybaseDialect();
		assertThat( dialect.getMultiKeyLoadSizingStrategy()
				.determineOptimalBatchLoadSize( 500, 10, false ) )
				.isEqualTo( 4 );
		assertThat( dialect.getBatchLoadSizingStrategy()
				.determineOptimalBatchLoadSize( 500, 10, true ) )
				.isEqualTo( 4 );
	}
}
