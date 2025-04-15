/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.hibernate.mapping.MetaAttribute;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MetaAttributeHelper {

	public static MetaAttribute toRealMetaAttribute(String name, Collection<?> values) {
		MetaAttribute attribute = new MetaAttribute(name);
		for (Iterator<?> iter = values.iterator(); iter.hasNext();) {
			SimpleMetaAttribute element = (SimpleMetaAttribute) iter.next();
			attribute.addValue(element.value);
		}
		
		return attribute;
	}


	public static MultiValuedMap<String, SimpleMetaAttribute> loadAndMergeMetaMap(
			Element classElement,
			MultiValuedMap<String, SimpleMetaAttribute> inheritedMeta) {
		return MetaAttributeHelper.mergeMetaMaps(
				loadMetaMap(classElement), 
				inheritedMeta);
	}

	public static MultiValuedMap<String, SimpleMetaAttribute> loadMetaMap(Element element) {
		MultiValuedMap<String, SimpleMetaAttribute> result = new HashSetValuedHashMap<String, SimpleMetaAttribute>();
		List<Element> metaAttributeList = new ArrayList<Element>();
		ArrayList<Element> metaNodes = getChildElements(element, "meta");
		for (Element metaNode : metaNodes) {
			metaAttributeList.add(metaNode);
		}
		for (Iterator<Element> iter = metaAttributeList.iterator(); iter.hasNext();) {
			Element metaAttribute = iter.next();
			String attribute = metaAttribute.getAttribute("attribute");
			String value = metaAttribute.getTextContent();
			String inheritStr= null;
			if (metaAttribute.hasAttribute("inherit")) {
				inheritStr = metaAttribute.getAttribute("inherit");
			}
			boolean inherit = true;
			if(inheritStr!=null) {
				inherit = Boolean.valueOf(inheritStr).booleanValue(); 
			}			
			SimpleMetaAttribute ma = new SimpleMetaAttribute(value, inherit);
			result.put(attribute, ma);
		}
		return result;
	}

	/**
	 * Merges a Multimap with inherited maps.
	 * Values specified always overrules/replaces the inherited values.
	 * 
	 * @param specific
	 * @param general
	 * @return a MultiMap with all values from local and extra values
	 * from inherited
	 */
	private static MultiValuedMap<String, SimpleMetaAttribute> mergeMetaMaps(
			MultiValuedMap<String, SimpleMetaAttribute> specific,
			MultiValuedMap<String, SimpleMetaAttribute> general) {
		MultiValuedMap<String, SimpleMetaAttribute> result = new HashSetValuedHashMap<String, MetaAttributeHelper.SimpleMetaAttribute>();
		copyMultiMap(result, specific);	
		if (general != null) {
			for (Iterator<String> iter = general.keySet().iterator();iter.hasNext();) {
				String key = iter.next();
				if (!specific.containsKey(key) ) {
					// inheriting a meta attribute only if it is inheritable
					Collection<SimpleMetaAttribute> ml = general.get(key);
					for (Iterator<SimpleMetaAttribute> iterator = ml.iterator(); iterator.hasNext();) {
						SimpleMetaAttribute element = iterator.next();
						if (element.inheritable) {
							result.put(key, element);
						}
					}
				}
			}
		}
	
		return result;
	
	}

    private static void copyMultiMap(
    		MultiValuedMap<String, SimpleMetaAttribute> destination, 
    		MultiValuedMap<String, SimpleMetaAttribute> specific) {
        for (Iterator<String> keyIterator = specific.keySet().iterator(); keyIterator.hasNext(); ) {
            String key = keyIterator.next();
            Collection<SimpleMetaAttribute> c = specific.get(key);
            for (Iterator<SimpleMetaAttribute> valueIterator = c.iterator(); valueIterator.hasNext(); ) 
                destination.put(key, valueIterator.next() );
        }
    }

	private static ArrayList<Element> getChildElements(Element parent, String tagName) {
		ArrayList<Element> result = new ArrayList<Element>();
		NodeList nodeList = parent.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Element) {
				if (tagName.equals(((Element)node).getTagName())) {
					result.add((Element)node);
				}
			}
		}
		return result;
	}
	
	 public static class SimpleMetaAttribute {
		String value;
		boolean inheritable = true;
		public SimpleMetaAttribute(String value, boolean inherit) {
			this.value = value;
			this.inheritable = inherit;
		}
		public String toString() {
			return value;
		}
	}
}
