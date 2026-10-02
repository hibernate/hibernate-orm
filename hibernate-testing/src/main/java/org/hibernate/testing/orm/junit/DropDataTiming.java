/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/**
 * Enumeration of when to drop test data automatically.
 *
 * @author inpink
 */
public enum DropDataTiming {
	/**
	 * Drop test data before each test method
	 */
	BEFORE_EACH,

	/**
	 * Drop test data after each test method
	 */
	AFTER_EACH,

	/**
	 * Drop test data before all test methods (once per test class)
	 */
	BEFORE_ALL,

	/**
	 * Drop test data after all test methods (once per test class)
	 */
	AFTER_ALL
}
