/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultsetsTrackingContainerTest {

	private ResultsetsTrackingContainer container;

	@Mock
	private Statement statement1;
	@Mock
	private Statement statement2;
	@Mock
	private Statement statement3;
	@Mock
	private ResultSet resultSet1;
	@Mock
	private ResultSet resultSet2;
	@Mock
	private ResultSet resultSet3;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		container = new ResultsetsTrackingContainer();
	}

	@Test
	void initialStateHasNoRegisteredResources() {
		assertFalse(container.hasRegisteredResources());
	}

	@Test
	void registeringSingleStatementShouldWork() {
		container.registerExpectingNew(statement1);
		assertTrue(container.hasRegisteredResources());
	}

	@Test
	void storingAssociatedResultSetForUnregisteredStatementShouldWork() {
		container.storeAssociatedResultset(statement1, resultSet1);
		assertTrue(container.hasRegisteredResources());
	}

	@Test
	void storingMultipleResultSetsForSameStatement() {
		container.registerExpectingNew(statement1);
		container.storeAssociatedResultset(statement1, resultSet1);
		container.storeAssociatedResultset(statement1, resultSet2);

		ResultSetsSet resultSets = container.getForResultSetRemoval(statement1);
		assertNotNull(resultSets);

		List<ResultSet> stored = new ArrayList<>();
		resultSets.forEachResultSet(stored::add);
		assertEquals(2, stored.size());
		assertTrue(stored.contains(resultSet1));
		assertTrue(stored.contains(resultSet2));
	}

	@Test
	void removingStatementShouldReturnAssociatedResultSets() {
		container.registerExpectingNew(statement1);
		container.storeAssociatedResultset(statement1, resultSet1);

		ResultSetsSet removed = container.remove(statement1);
		assertNotNull(removed);

		List<ResultSet> stored = new ArrayList<>();
		removed.forEachResultSet(stored::add);
		assertEquals(1, stored.size());
		assertTrue(stored.contains(resultSet1));
		assertFalse(container.hasRegisteredResources());
	}

	@Test
	void removingNonExistentStatementShouldReturnNull() {
		assertNull(container.remove(statement1));
	}

	@Test
	void handlingMultipleStatements() {
		container.registerExpectingNew(statement1);
		container.registerExpectingNew(statement2);
		container.storeAssociatedResultset(statement1, resultSet1);
		container.storeAssociatedResultset(statement2, resultSet2);

		assertTrue(container.hasRegisteredResources());

		List<Statement> processedStatements = new ArrayList<>();
		List<ResultSetsSet> processedResultSets = new ArrayList<>();

		container.forEach((stmt, results) -> {
			processedStatements.add(stmt);
			processedResultSets.add(results);
		});

		assertEquals(2, processedStatements.size());
		assertTrue(processedStatements.contains(statement1));
		assertTrue(processedStatements.contains(statement2));
	}

	@Test
	void clearingShouldRemoveAllResources() {
		container.registerExpectingNew(statement1);
		container.registerExpectingNew(statement2);
		container.storeAssociatedResultset(statement1, resultSet1);
		container.storeAssociatedResultset(statement2, resultSet2);

		container.clear();

		assertFalse(container.hasRegisteredResources());
		assertNull(container.getForResultSetRemoval(statement1));
		assertNull(container.getForResultSetRemoval(statement2));
	}

	@Test
	void trickleDownShouldMoveEntryFromXrefToMainSlot() {
		container.registerExpectingNew(statement1);
		container.registerExpectingNew(statement2);
		container.storeAssociatedResultset(statement1, resultSet1);
		container.storeAssociatedResultset(statement2, resultSet2);

		container.remove(statement1); // This should trigger trickle down

		assertTrue(container.hasRegisteredResources());
		assertNotNull(container.getForResultSetRemoval(statement2));

		List<Statement> processedStatements = new ArrayList<>();
		container.forEach((stmt, results) -> processedStatements.add(stmt));

		assertEquals(1, processedStatements.size());
		assertEquals(statement2, processedStatements.get(0));
	}

	@Test
	void getForRemovalShouldReturnCorrectResultSets() {
		container.registerExpectingNew(statement1);
		container.storeAssociatedResultset(statement1, resultSet1);

		ResultSetsSet resultSets = container.getForResultSetRemoval(statement1);
		assertNotNull(resultSets);

		List<ResultSet> stored = new ArrayList<>();
		resultSets.forEachResultSet(stored::add);
		assertEquals(1, stored.size());
		assertTrue(stored.contains(resultSet1));
	}

	@Test
	void forEachShouldProcessAllEntries() {
		container.registerExpectingNew(statement1);
		container.registerExpectingNew(statement2);
		container.storeAssociatedResultset(statement1, resultSet1);
		container.storeAssociatedResultset(statement2, resultSet2);

		@SuppressWarnings("unchecked")
		BiConsumer<Statement, ResultSetsSet> mockConsumer = mock(BiConsumer.class);

		container.forEach(mockConsumer);

		verify(mockConsumer, times(2)).accept(any(), any());
		verify(mockConsumer).accept(eq(statement1), any());
		verify(mockConsumer).accept(eq(statement2), any());
	}
}
