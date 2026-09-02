/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.GenerationType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies community Dialect native identifier-generation answers and the
/// inherited TimesTen default.
///
/// @author Steve Ebersole
public class NativeIdentifierGenerationTest {
	@Test
	void communityDialectsPreserveTheirTypedValues() {
		assertThat( new PostgreSQLLegacyDialect().getNativeValueGenerationStrategy() )
				.isEqualTo( GenerationType.SEQUENCE );
		assertThat( new CockroachLegacyDialect().getNativeValueGenerationStrategy() )
				.isEqualTo( GenerationType.SEQUENCE );
		assertThat( new GaussDBDialect().getNativeValueGenerationStrategy() )
				.isEqualTo( GenerationType.SEQUENCE );
		assertThat( new OracleLegacyDialect().getNativeValueGenerationStrategy() )
				.isEqualTo( GenerationType.SEQUENCE );
		assertThat( new CacheDialect().getNativeValueGenerationStrategy() )
				.isEqualTo( GenerationType.IDENTITY );
	}

	@Test
	void timesTenUsesSequenceGeneration() {
		assertThat( new TimesTenDialect().getNativeValueGenerationStrategy() )
				.isEqualTo( GenerationType.SEQUENCE );
	}
}
