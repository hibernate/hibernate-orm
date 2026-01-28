/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.apache.commons.collections4.MultiValuedMap;
import org.hibernate.tool.reveng.internal.core.strategy.MetaAttributeHelper.SimpleMetaAttribute;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetaAttributeHelperTest {

	@Test
	public void testLoadMetaMap() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		String XML = "<element>                                         " +
					"  <meta attribute='blah' inherit='true'>foo</meta>" +
					"</element>                                        ";
		Document document = db.parse(new ByteArrayInputStream( XML.getBytes()));
		MultiValuedMap<String, SimpleMetaAttribute> mm = MetaAttributeHelper.loadMetaMap(document.getDocumentElement());
		assertEquals(1, mm.size());
		Collection<SimpleMetaAttribute> attributeList = mm.get("blah");
		assertEquals(1, attributeList.size());
		Optional<SimpleMetaAttribute> first = attributeList.stream().findFirst();
		assertTrue(first.isPresent());
		SimpleMetaAttribute attribute = first.get();
		assertTrue( attribute.inheritable );
		assertEquals("foo", attribute.value);
	}

}
