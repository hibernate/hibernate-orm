/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.hibernate.mapping.MetaAttribute;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the export/java MetaAttributeHelper (different from core/strategy MetaAttributeHelper).
 */
public class MetaAttributeHelperJavaTest {

	@Test
	public void testGetMetaAsStringFromCollectionNull() {
		assertEquals("", MetaAttributeHelper.getMetaAsString((java.util.Collection<?>) null, ","));
	}

	@Test
	public void testGetMetaAsStringFromCollectionEmpty() {
		assertEquals("", MetaAttributeHelper.getMetaAsString(Collections.emptyList(), ","));
	}

	@Test
	public void testGetMetaAsStringFromCollectionSingle() {
		assertEquals("value", MetaAttributeHelper.getMetaAsString(List.of("value"), ","));
	}

	@Test
	public void testGetMetaAsStringFromCollectionMultiple() {
		assertEquals("a,b,c", MetaAttributeHelper.getMetaAsString(Arrays.asList("a", "b", "c"), ","));
	}

	@Test
	public void testGetMetaAsStringFromMetaAttributeNull() {
		assertNull(MetaAttributeHelper.getMetaAsString((MetaAttribute) null, ","));
	}

	@Test
	public void testGetMetaAsStringFromMetaAttribute() {
		MetaAttribute attr = new MetaAttribute("test");
		attr.addValue("hello");
		attr.addValue("world");
		assertEquals("hello,world", MetaAttributeHelper.getMetaAsString(attr, ","));
	}

	@Test
	public void testGetMetaAsStringNoSeparator() {
		MetaAttribute attr = new MetaAttribute("test");
		attr.addValue("hello");
		assertEquals("hello", MetaAttributeHelper.getMetaAsString(attr));
	}

	@Test
	public void testGetMetaAsStringNullNoSeparator() {
		assertEquals("", MetaAttributeHelper.getMetaAsString((MetaAttribute) null));
	}

	@Test
	public void testGetMetaAsBoolNullMetaAttribute() {
		assertTrue(MetaAttributeHelper.getMetaAsBool((MetaAttribute) null, true));
		assertFalse(MetaAttributeHelper.getMetaAsBool((MetaAttribute) null, false));
	}

	@Test
	public void testGetMetaAsBoolTrue() {
		MetaAttribute attr = new MetaAttribute("test");
		attr.addValue("true");
		assertTrue(MetaAttributeHelper.getMetaAsBool(attr, false));
	}

	@Test
	public void testGetMetaAsBoolFalse() {
		MetaAttribute attr = new MetaAttribute("test");
		attr.addValue("false");
		assertFalse(MetaAttributeHelper.getMetaAsBool(attr, true));
	}

	@Test
	public void testGetMetaAsBoolFromCollectionEmpty() {
		assertTrue(MetaAttributeHelper.getMetaAsBool(Collections.emptyList(), true));
	}

	@Test
	public void testGetMetaAsBoolFromCollectionWithValue() {
		assertTrue(MetaAttributeHelper.getMetaAsBool(List.of("true"), false));
		assertFalse(MetaAttributeHelper.getMetaAsBool(List.of("false"), true));
	}
}
