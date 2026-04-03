/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IssueTest {

	@Test
	public void testConstructorAndGetters() {
		Issue issue = new Issue("TABLE_WITHOUT_PK", Issue.HIGH_PRIORITY, "Table 'foo' has no primary key");
		assertEquals("Table 'foo' has no primary key", issue.getDescription());
		assertEquals(Issue.HIGH_PRIORITY, issue.getPriority());
	}

	@Test
	public void testToString() {
		Issue issue = new Issue("MISSING_INDEX", Issue.NORMAL_PRIORITY, "No index on column 'bar'");
		assertEquals("MISSING_INDEX:No index on column 'bar'", issue.toString());
	}

	@Test
	public void testPriorityConstants() {
		assertEquals(100, Issue.HIGH_PRIORITY);
		assertEquals(50, Issue.NORMAL_PRIORITY);
		assertEquals(0, Issue.LOW_PRIORITY);
	}

	@Test
	public void testLowPriority() {
		Issue issue = new Issue("INFO", Issue.LOW_PRIORITY, "Minor suggestion");
		assertEquals(Issue.LOW_PRIORITY, issue.getPriority());
	}
}
