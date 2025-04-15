/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2020-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.reveng.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections4.MultiValuedMap;
import org.hibernate.tool.internal.reveng.strategy.MetaAttributeHelper.SimpleMetaAttribute;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class MetaAttributeHelperTest {
	
	private static String XML = 
			"<element>                                         " +
			"  <meta attribute='blah' inherit='true'>foo</meta>" +
			"</element>                                        ";
	
	@Test
	public void testLoadMetaMap() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(new ByteArrayInputStream(XML.getBytes()));
		MultiValuedMap<String, SimpleMetaAttribute> mm = MetaAttributeHelper.loadMetaMap(document.getDocumentElement());
		assertEquals(1, mm.size());
		Collection<SimpleMetaAttribute> attributeList = mm.get("blah");
		assertEquals(1, attributeList.size());
		Optional<SimpleMetaAttribute> first = attributeList.stream().findFirst();
		assertTrue(first.isPresent());
		SimpleMetaAttribute attribute = first.get();
		assertEquals(true, attribute.inheritable);
		assertEquals("foo", attribute.value);
	}

}
