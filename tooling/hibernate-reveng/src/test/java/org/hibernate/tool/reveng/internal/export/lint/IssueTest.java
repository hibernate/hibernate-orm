/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IssueTest {

	@Test
	public void testConstants() {
		assertEquals(100, Issue.HIGH_PRIORITY);
		assertEquals(50, Issue.NORMAL_PRIORITY);
		assertEquals(0, Issue.LOW_PRIORITY);
	}

	@Test
	public void testConstructorAndGetters() {
		Issue issue = new Issue("TEST_TYPE", Issue.HIGH_PRIORITY, "Test description");
		assertEquals("Test description", issue.getDescription());
		assertEquals(100, issue.getPriority());
	}

	@Test
	public void testToString() {
		Issue issue = new Issue("WARN", Issue.NORMAL_PRIORITY, "something went wrong");
		assertEquals("WARN:something went wrong", issue.toString());
	}

	@Test
	public void testCreateInstanceFactory() {
		HbmLint lint = HbmLint.createInstance();
		assertEquals(4, lint.detectors.length);
	}

	@Test
	public void testReportAndGetResults() {
		HbmLint lint = new HbmLint(new Detector[0]);
		assertEquals(0, lint.getResults().size());
		lint.reportIssue(new Issue("T", Issue.LOW_PRIORITY, "d"));
		assertEquals(1, lint.getResults().size());
		lint.reportIssue(new Issue("T2", Issue.HIGH_PRIORITY, "d2"));
		assertEquals(2, lint.getResults().size());
	}
}
