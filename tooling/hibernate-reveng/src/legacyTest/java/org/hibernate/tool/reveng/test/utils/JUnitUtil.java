/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.test.utils;

import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JUnitUtil {

	public static void assertIteratorContainsExactly(
			String reason,
			Iterator<?> iterator,
			int expectedAmountOfElements) {
		int actualAmountOfElements = 0;
		while (iterator.hasNext() &&
			actualAmountOfElements <= expectedAmountOfElements) {
			actualAmountOfElements++;
			iterator.next();
		}
		if (expectedAmountOfElements != actualAmountOfElements) {
			throw new AssertionFailedError(reason, expectedAmountOfElements, actualAmountOfElements);
		}
	}

	public static void assertIsNonEmptyFile(File file) {
		assertTrue(file.exists(), file + " does not exist");
		assertTrue(file.isFile(), file + " not a file");
		assertTrue(file.length()>0, file + " does not have any contents");
	}

}
