/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * See {@link ResultSetsSet}: a custom data container,
 * we should be able to fully test this via simple unit testing.
 */
public class ResultSetsSetTest {

	private ResultSetsSet resultSetsSet;
	private ResultSet mockResultSet1;
	private ResultSet mockResultSet2;
	private ResultSet mockResultSet3;

	@BeforeEach
	void setUp() {
		resultSetsSet = new ResultSetsSet();
		mockResultSet1 = mock(ResultSet.class);
		mockResultSet2 = mock(ResultSet.class);
		mockResultSet3 = mock(ResultSet.class);
	}

	@Test
	void testInitialStateIsEmpty() {
		assertTrue(resultSetsSet.isEmpty());
	}

	@Test
	void testStoreFirstResultSet() {
		resultSetsSet.storeResultSet(mockResultSet1);
		assertFalse(resultSetsSet.isEmpty());
	}

	@Test
	void testStoreDuplicateResultSet() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet1);

		// Store same ResultSet twice should be a no-op
		List<ResultSet> stored = new ArrayList<>();
		resultSetsSet.forEachResultSet(stored::add);

		assertEquals(1, stored.size());
		assertSame(mockResultSet1, stored.get(0));

		//The same after the first element is inserted:
		resultSetsSet.storeResultSet(mockResultSet2);
		resultSetsSet.storeResultSet(mockResultSet2);

		stored.clear();
		resultSetsSet.forEachResultSet(stored::add);
		assertEquals(2, stored.size());
		assertTrue(stored.contains(mockResultSet1));

	}

	@Test
	void testStoreMultipleResultSets() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);
		resultSetsSet.storeResultSet(mockResultSet3);

		List<ResultSet> stored = new ArrayList<>();
		resultSetsSet.forEachResultSet(stored::add);

		assertEquals(3, stored.size());
		assertTrue(stored.contains(mockResultSet1));
		assertTrue(stored.contains(mockResultSet2));
		assertTrue(stored.contains(mockResultSet3));
	}

	@Test
	void testRemoveExistingResultSet() {
		resultSetsSet.storeResultSet(mockResultSet1);
		ResultSet removed = resultSetsSet.removeResultSet(mockResultSet1);

		assertSame(mockResultSet1, removed);
		assertTrue(resultSetsSet.isEmpty());
	}

	@Test
	void testRemoveNonExistingResultSet() {
		resultSetsSet.storeResultSet(mockResultSet1);
		ResultSet removed = resultSetsSet.removeResultSet(mockResultSet2);

		assertNull(removed);
		assertFalse(resultSetsSet.isEmpty());
	}

	@Test
	void testClear() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);

		resultSetsSet.clear();
		assertTrue(resultSetsSet.isEmpty());
	}

	@Test
	void testForEachResultSet() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);
		resultSetsSet.storeResultSet(mockResultSet3);

		@SuppressWarnings("unchecked")
		Consumer<ResultSet> mockConsumer = mock(Consumer.class);
		resultSetsSet.forEachResultSet(mockConsumer);

		verify(mockConsumer).accept(mockResultSet1);
		verify(mockConsumer).accept(mockResultSet2);
		verify(mockConsumer).accept(mockResultSet3);
		verifyNoMoreInteractions(mockConsumer);
	}

	@Test
	void testScaleDownWhenRemovingFirst() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);

		// Remove first ResultSet
		resultSetsSet.removeResultSet(mockResultSet1);

		// Verify second ResultSet got moved to first position
		List<ResultSet> remaining = new ArrayList<>();
		resultSetsSet.forEachResultSet(remaining::add);

		assertEquals(1, remaining.size());
		assertSame(mockResultSet2, remaining.get(0));
	}

	@Test
	void testScaleUpDown() {
		assertTrue(resultSetsSet.isEmpty());
		resultSetsSet.storeResultSet(mockResultSet1);
		assertFalse(resultSetsSet.isEmpty());
		resultSetsSet.storeResultSet(mockResultSet2);
		resultSetsSet.storeResultSet(mockResultSet3);
		resultSetsSet.storeResultSet(mockResultSet3);//intentional duplicate

		resultSetsSet.removeResultSet(mockResultSet1);
		resultSetsSet.removeResultSet(mockResultSet2);
		assertFalse(resultSetsSet.isEmpty());

		// Now we should have only mockResultSet3
		List<ResultSet> remaining = new ArrayList<>();
		resultSetsSet.forEachResultSet(remaining::add);

		assertEquals(1, remaining.size());
		assertSame(mockResultSet3, remaining.get(0));

		resultSetsSet.storeResultSet(mockResultSet3);//another duplicate, different internal slot
		resultSetsSet.removeResultSet(mockResultSet3);
		assertTrue(resultSetsSet.isEmpty());
		remaining.clear();
		resultSetsSet.forEachResultSet(remaining::add);
		assertEquals(0, remaining.size());
	}

	@Test
	void testRemoveFromEmptySet() {
		ResultSet removed = resultSetsSet.removeResultSet(mockResultSet1);
		assertNull(removed);
		assertTrue(resultSetsSet.isEmpty());
	}

	@Test
	void testForEachOnEmptySet() {
		@SuppressWarnings("unchecked")
		Consumer<ResultSet> mockConsumer = mock(Consumer.class);
		resultSetsSet.forEachResultSet(mockConsumer);

		verifyNoInteractions(mockConsumer);
	}

	@Test
	void testScaleDownWithMultipleRemovals() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);
		resultSetsSet.storeResultSet(mockResultSet3);

		// Remove first ResultSet twice
		ResultSet removed1 = resultSetsSet.removeResultSet(mockResultSet1);
		ResultSet removed2 = resultSetsSet.removeResultSet(mockResultSet1);

		assertSame(mockResultSet1, removed1);
		assertNull(removed2);

		List<ResultSet> remaining = new ArrayList<>();
		resultSetsSet.forEachResultSet(remaining::add);
		assertEquals(2, remaining.size());
	}

	@Test
	void testClearAndReuse() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);

		resultSetsSet.clear();
		assertTrue(resultSetsSet.isEmpty());

		// Test that we can reuse the set after clearing
		resultSetsSet.storeResultSet(mockResultSet3);
		assertFalse(resultSetsSet.isEmpty());

		List<ResultSet> stored = new ArrayList<>();
		resultSetsSet.forEachResultSet(stored::add);
		assertEquals(1, stored.size());
		assertSame(mockResultSet3, stored.get(0));
	}

	@Test
	void testRemoveAndRestore() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);

		ResultSet removed = resultSetsSet.removeResultSet(mockResultSet1);
		assertSame(mockResultSet1, removed);

		// Restore the removed ResultSet
		resultSetsSet.storeResultSet(mockResultSet1);

		List<ResultSet> stored = new ArrayList<>();
		resultSetsSet.forEachResultSet(stored::add);
		assertEquals(2, stored.size());
		assertTrue(stored.contains(mockResultSet1));
		assertTrue(stored.contains(mockResultSet2));
	}

	@Test
	void testStoreRemoveStore() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.removeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);

		List<ResultSet> stored = new ArrayList<>();
		resultSetsSet.forEachResultSet(stored::add);

		assertEquals(1, stored.size());
		assertSame(mockResultSet2, stored.get(0));
	}

	@Test
	void testRemoveNonFirstWhenMultiple() {
		resultSetsSet.storeResultSet(mockResultSet1);
		resultSetsSet.storeResultSet(mockResultSet2);
		resultSetsSet.storeResultSet(mockResultSet3);

		ResultSet removed = resultSetsSet.removeResultSet(mockResultSet2);
		assertSame(mockResultSet2, removed);

		List<ResultSet> remaining = new ArrayList<>();
		resultSetsSet.forEachResultSet(remaining::add);
		assertEquals(2, remaining.size());
		assertTrue(remaining.contains(mockResultSet1));
		assertTrue(remaining.contains(mockResultSet3));
	}

}
