/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that environment characteristics used by provider tests remain
/// test-local instead of becoming production Dialect contracts.
///
/// @author Steve Ebersole
class ExampleDatabaseCharacteristicsTest {
	@Test
	void ownsEnvironmentCharacteristicsInTestSource() {
		final var environment = new EnvironmentCharacteristics( false, true, false );

		assertFalse( environment.supportsForwardOnlyCursorPositioning() );
		assertTrue( environment.readCommittedWritersBlockReaders() );
		assertFalse( environment.repeatableReadReadersBlockWriters() );
	}
	private record EnvironmentCharacteristics(
			boolean supportsForwardOnlyCursorPositioning,
			boolean readCommittedWritersBlockReaders,
			boolean repeatableReadReadersBlockWriters) {
	}
}
