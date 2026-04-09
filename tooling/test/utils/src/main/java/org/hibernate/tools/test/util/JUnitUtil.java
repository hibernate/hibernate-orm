/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tools.test.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Iterator;

import org.opentest4j.AssertionFailedError;

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
