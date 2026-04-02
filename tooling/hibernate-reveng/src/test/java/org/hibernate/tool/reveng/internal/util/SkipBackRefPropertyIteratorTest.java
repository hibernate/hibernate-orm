/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.hibernate.mapping.Property;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SkipBackRefPropertyIteratorTest {

	private Property createProperty(String name, boolean backRef) {
		Property p = new Property();
		// backRef property names start with underscore by Hibernate convention
		if (backRef) {
			p.setName("_" + name + "BackRef");
		} else {
			p.setName(name);
		}
		return p;
	}

	@Test
	public void testEmptyIterator() {
		SkipBackRefPropertyIterator iter = new SkipBackRefPropertyIterator(Collections.emptyIterator());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testAllNormalProperties() {
		List<Property> props = Arrays.asList(
				createProperty("name", false),
				createProperty("age", false)
		);
		SkipBackRefPropertyIterator iter = new SkipBackRefPropertyIterator(props.iterator());

		assertTrue(iter.hasNext());
		assertEquals("name", iter.next().getName());
		assertTrue(iter.hasNext());
		assertEquals("age", iter.next().getName());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testRemoveThrowsUnsupported() {
		List<Property> props = List.of(createProperty("name", false));
		SkipBackRefPropertyIterator iter = new SkipBackRefPropertyIterator(props.iterator());
		assertThrows(UnsupportedOperationException.class, iter::remove);
	}

	@Test
	public void testNextOnEmptyThrows() {
		SkipBackRefPropertyIterator iter = new SkipBackRefPropertyIterator(Collections.emptyIterator());
		assertThrows(NoSuchElementException.class, iter::next);
	}

	@Test
	public void testSingleNormalProperty() {
		List<Property> props = List.of(createProperty("id", false));
		SkipBackRefPropertyIterator iter = new SkipBackRefPropertyIterator(props.iterator());
		assertTrue(iter.hasNext());
		assertEquals("id", iter.next().getName());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testNextWithoutHasNext() {
		List<Property> props = Arrays.asList(
				createProperty("name", false),
				createProperty("age", false)
		);
		SkipBackRefPropertyIterator iter = new SkipBackRefPropertyIterator(props.iterator());
		assertEquals("name", iter.next().getName());
		assertEquals("age", iter.next().getName());
	}
}
