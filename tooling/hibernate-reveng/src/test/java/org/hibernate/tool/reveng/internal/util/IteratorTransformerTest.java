/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IteratorTransformerTest {

	private IteratorTransformer<Integer> createToStringTransformer(List<Integer> list) {
		return new IteratorTransformer<>(list.iterator()) {
			@Override
			public String transform(Integer object) {
				return "item-" + object;
			}
		};
	}

	@Test
	public void testHasNextAndNext() {
		List<Integer> list = Arrays.asList(1, 2, 3);
		IteratorTransformer<Integer> transformer = createToStringTransformer(list);

		assertTrue(transformer.hasNext());
		assertEquals("item-1", transformer.next());
		assertTrue(transformer.hasNext());
		assertEquals("item-2", transformer.next());
		assertTrue(transformer.hasNext());
		assertEquals("item-3", transformer.next());
		assertFalse(transformer.hasNext());
	}

	@Test
	public void testEmptyIterator() {
		IteratorTransformer<Integer> transformer = createToStringTransformer(List.of());
		assertFalse(transformer.hasNext());
	}

	@Test
	public void testRemove() {
		List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
		IteratorTransformer<Integer> transformer = createToStringTransformer(list);
		transformer.next();
		transformer.remove();
		assertEquals(2, list.size());
	}

	@Test
	public void testTransformWithCustomLogic() {
		List<String> list = Arrays.asList("hello", "world");
		IteratorTransformer<String> transformer = new IteratorTransformer<>(list.iterator()) {
			@Override
			public String transform(String object) {
				return object.toUpperCase();
			}
		};
		assertEquals("HELLO", transformer.next());
		assertEquals("WORLD", transformer.next());
	}

	@Test
	public void testNoSuchElement() {
		IteratorTransformer<Integer> transformer = createToStringTransformer(List.of());
		assertThrows(NoSuchElementException.class, transformer::next);
	}
}
