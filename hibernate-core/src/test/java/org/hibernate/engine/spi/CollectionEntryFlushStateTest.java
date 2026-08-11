/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.AssertionFailure;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the compact traversal state retained by a collection entry during one flush cycle.
///
/// @author Steve Ebersole
class CollectionEntryFlushStateTest {
	@Test
	void resetsReachedAndProcessedStateTogether() {
		final var entry = new CollectionEntry( (CollectionPersister) null, (Object) null );

		assertFalse( entry.wasReachedDuringFlush() );
		assertFalse( entry.wasProcessedDuringFlush() );

		entry.markReachedDuringFlush();
		entry.markProcessedDuringFlush();

		assertTrue( entry.wasReachedDuringFlush() );
		assertTrue( entry.wasProcessedDuringFlush() );

		entry.resetFlushState();

		assertFalse( entry.wasReachedDuringFlush() );
		assertFalse( entry.wasProcessedDuringFlush() );
	}

	@Test
	void rejectsDuplicateProcessing() {
		final var entry = new CollectionEntry( (CollectionPersister) null, (Object) null );
		entry.markProcessedDuringFlush();

		assertThrows( AssertionFailure.class, entry::markProcessedDuringFlush );
	}
}
