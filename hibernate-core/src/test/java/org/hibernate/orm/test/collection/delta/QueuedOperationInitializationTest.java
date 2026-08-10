/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.collection.spi.PersistentList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests that initialization captures loaded state before applying queued commands.
///
/// @author Steve Ebersole
public class QueuedOperationInitializationTest {
	@Test
	void queuedCommandsAreAppliedAndClearedAfterSnapshotCapture() {
		final var collection = new TestPersistentList();
		collection.queueAppend( "queued" );

		assertFalse( collection.afterInitialize() );
		assertTrue( collection.wasInitialized() );
		assertTrue( collection.hasQueuedOperations() );
		assertEquals( List.of( "loaded" ), collection.currentState() );

		final var loadedSnapshot = List.copyOf( collection.currentState() );
		collection.afterInitializationSnapshot();

		assertEquals( List.of( "loaded" ), loadedSnapshot );
		assertEquals( List.of( "loaded", "queued" ), collection.currentState() );
		assertFalse( collection.hasQueuedOperations() );
	}

	private static class TestPersistentList extends PersistentList<String> {
		private TestPersistentList() {
			list = new ArrayList<>( List.of( "loaded" ) );
		}

		private void queueAppend(String value) {
			queueOperation( new SimpleAdd( value ) );
		}

		private List<String> currentState() {
			return list;
		}
	}
}
