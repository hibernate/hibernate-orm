/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/// Verifies functional-dependency profiles and maintained supply points.
///
/// @author Steve Ebersole
public class FunctionalDependencyAnalysisSupportTest {
	@Test
	void namedProfilesPreserveTheCapabilityHierarchy() {
		assertProfile( FunctionalDependencyAnalysisSupport.NONE, false, false, false );
		assertProfile( FunctionalDependencyAnalysisSupport.TABLE_REFERENCE, true, false, false );
		assertProfile( FunctionalDependencyAnalysisSupport.TABLE_GROUP, true, true, false );
		assertProfile( FunctionalDependencyAnalysisSupport.TABLE_GROUP_AND_CONSTANTS, true, true, true );
	}

	@Test
	void invalidAdHocProfilesAreRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new FunctionalDependencyAnalysisSupport( false, true, false ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new FunctionalDependencyAnalysisSupport( true, false, true ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new FunctionalDependencyAnalysisSupport( false, false, true ) );
	}

	private static void assertProfile(
			FunctionalDependencyAnalysisSupport support,
			boolean analysis,
			boolean tableGroups,
			boolean constants) {
		assertThat( support.supportsAnalysis() ).isEqualTo( analysis );
		assertThat( support.supportsTableGroups() ).isEqualTo( tableGroups );
		assertThat( support.supportsConstants() ).isEqualTo( constants );
	}
}
