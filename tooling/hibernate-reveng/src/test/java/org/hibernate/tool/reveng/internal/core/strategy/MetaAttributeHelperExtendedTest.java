/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.tool.reveng.internal.core.strategy.MetaAttributeHelper.SimpleMetaAttribute;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetaAttributeHelperExtendedTest {

	private Element parseXml(String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
		return doc.getDocumentElement();
	}

	@Test
	public void testToRealMetaAttribute() {
		List<SimpleMetaAttribute> values = List.of(
				new SimpleMetaAttribute("value1", true),
				new SimpleMetaAttribute("value2", false)
		);
		MetaAttribute attr = MetaAttributeHelper.toRealMetaAttribute("testAttr", values);
		assertNotNull(attr);
		assertEquals("testAttr", attr.getName());
		assertTrue(attr.getValues().contains("value1"));
		assertTrue(attr.getValues().contains("value2"));
	}

	@Test
	public void testLoadMetaMapMultipleAttributes() throws Exception {
		String xml = "<element>"
				+ "  <meta attribute='scope'>public</meta>"
				+ "  <meta attribute='description'>A test class</meta>"
				+ "</element>";
		MultiValuedMap<String, SimpleMetaAttribute> mm = MetaAttributeHelper.loadMetaMap(parseXml(xml));
		assertEquals(2, mm.size());
		assertFalse(mm.get("scope").isEmpty());
		assertFalse(mm.get("description").isEmpty());
	}

	@Test
	public void testLoadMetaMapInheritFalse() throws Exception {
		String xml = "<element>"
				+ "  <meta attribute='scope' inherit='false'>public</meta>"
				+ "</element>";
		MultiValuedMap<String, SimpleMetaAttribute> mm = MetaAttributeHelper.loadMetaMap(parseXml(xml));
		SimpleMetaAttribute attr = mm.get("scope").iterator().next();
		assertFalse(attr.inheritable);
		assertEquals("public", attr.value);
	}

	@Test
	public void testLoadMetaMapNoInheritAttribute() throws Exception {
		String xml = "<element>"
				+ "  <meta attribute='scope'>public</meta>"
				+ "</element>";
		MultiValuedMap<String, SimpleMetaAttribute> mm = MetaAttributeHelper.loadMetaMap(parseXml(xml));
		SimpleMetaAttribute attr = mm.get("scope").iterator().next();
		assertTrue(attr.inheritable);
	}

	@Test
	public void testLoadMetaMapEmpty() throws Exception {
		String xml = "<element></element>";
		MultiValuedMap<String, SimpleMetaAttribute> mm = MetaAttributeHelper.loadMetaMap(parseXml(xml));
		assertTrue(mm.isEmpty());
	}

	@Test
	public void testLoadAndMergeMetaMapWithInheritedMeta() throws Exception {
		String xml = "<element>"
				+ "  <meta attribute='local'>localValue</meta>"
				+ "</element>";

		MultiValuedMap<String, SimpleMetaAttribute> inherited = new HashSetValuedHashMap<>();
		inherited.put("inherited", new SimpleMetaAttribute("inheritedValue", true));
		inherited.put("nonInherited", new SimpleMetaAttribute("shouldNotAppear", false));

		MultiValuedMap<String, SimpleMetaAttribute> result =
				MetaAttributeHelper.loadAndMergeMetaMap(parseXml(xml), inherited);

		assertFalse(result.get("local").isEmpty());
		assertFalse(result.get("inherited").isEmpty());
		assertTrue(result.get("nonInherited").isEmpty());
	}

	@Test
	public void testLoadAndMergeMetaMapLocalOverridesInherited() throws Exception {
		String xml = "<element>"
				+ "  <meta attribute='scope'>private</meta>"
				+ "</element>";

		MultiValuedMap<String, SimpleMetaAttribute> inherited = new HashSetValuedHashMap<>();
		inherited.put("scope", new SimpleMetaAttribute("public", true));

		MultiValuedMap<String, SimpleMetaAttribute> result =
				MetaAttributeHelper.loadAndMergeMetaMap(parseXml(xml), inherited);

		Collection<SimpleMetaAttribute> scopeValues = result.get("scope");
		assertEquals(1, scopeValues.size());
		assertEquals("private", scopeValues.iterator().next().value);
	}

	@Test
	public void testLoadAndMergeMetaMapWithNullInherited() throws Exception {
		String xml = "<element>"
				+ "  <meta attribute='scope'>public</meta>"
				+ "</element>";

		MultiValuedMap<String, SimpleMetaAttribute> result =
				MetaAttributeHelper.loadAndMergeMetaMap(parseXml(xml), null);

		assertFalse(result.get("scope").isEmpty());
	}

	@Test
	public void testSimpleMetaAttributeToString() {
		SimpleMetaAttribute attr = new SimpleMetaAttribute("testValue", true);
		assertEquals("testValue", attr.toString());
	}

	@Test
	public void testLoadMetaMapWithNonMetaChildren() throws Exception {
		String xml = "<element>"
				+ "  <other>ignored</other>"
				+ "  <meta attribute='scope'>public</meta>"
				+ "  <another>also ignored</another>"
				+ "</element>";
		MultiValuedMap<String, SimpleMetaAttribute> mm = MetaAttributeHelper.loadMetaMap(parseXml(xml));
		assertEquals(1, mm.size());
	}
}
