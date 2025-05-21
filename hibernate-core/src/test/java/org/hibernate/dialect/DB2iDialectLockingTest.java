/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialect( DB2iDialect.class )
class DB2iDialectLockingTest {

	private static final String EXPECTED_FOR_UPDATE = " for update with rs";
	private static final String EXPECTED_FOR_UPDATE_SKIP_LOCK = " for update with rs skip locked data";

	private DB2iDialect dialect;

	@BeforeEach
	void setUp() {
		dialect = new DB2iDialect();
	}

	@Test
	@JiraKey("HHH-18560")
	void getForUpdateString() {
		String actual = dialect.getForUpdateString();
		assertEquals(EXPECTED_FOR_UPDATE, actual);
	}

	@Test
	@JiraKey("HHH-18560")
	void getForUpdateSkipLockedString() {
		String actual = dialect.getForUpdateSkipLockedString();
		assertEquals(EXPECTED_FOR_UPDATE_SKIP_LOCK, actual);
	}

	@Test
	@JiraKey("HHH-18560")
	void testGetForUpdateSkipLockedString() {
		String actual = dialect.getForUpdateSkipLockedString("alias");
		assertEquals(EXPECTED_FOR_UPDATE_SKIP_LOCK, actual);
	}

	@Test
	@JiraKey("HHH-18560")
	void getWriteLockString_skiplocked() {
		String actual = dialect.getWriteLockString(-2);
		assertEquals(EXPECTED_FOR_UPDATE_SKIP_LOCK, actual);
	}

	@Test
	@JiraKey("HHH-18560")
	void getWriteLockString() {
		String actual = dialect.getWriteLockString(0);
		assertEquals(EXPECTED_FOR_UPDATE, actual);
	}

	@Test
	@JiraKey("HHH-18560")
	void getReadLockString() {
		String actual = dialect.getReadLockString(0);
		assertEquals(EXPECTED_FOR_UPDATE, actual);
	}

	@Test
	@JiraKey("HHH-18560")
	void getReadLockString_skipLocked() {
		String actual = dialect.getReadLockString(-2);
		assertEquals(EXPECTED_FOR_UPDATE_SKIP_LOCK, actual);
	}
}
