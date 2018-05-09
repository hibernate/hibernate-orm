package org.hibernate.tool.internal.reveng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.tool.internal.util.MultiMapUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MetaAttributeHelper {

	public static MetaAttribute toRealMetaAttribute(String name, List<?> values) {
		MetaAttribute attribute = new MetaAttribute(name);
		for (Iterator<?> iter = values.iterator(); iter.hasNext();) {
			SimpleMetaAttribute element = (SimpleMetaAttribute) iter.next();
			attribute.addValue(element.value);
		}
		
		return attribute;
	}


	static MultiMap loadAndMergeMetaMap(
			Element classElement,
			MultiMap inheritedMeta) {
		return MetaAttributeHelper.mergeMetaMaps(
				loadMetaMap(classElement), 
				inheritedMeta);
	}

	static MultiMap loadMetaMap(Element element) {
		MultiMap result = new MultiValueMap();
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
	private static MultiMap mergeMetaMaps(MultiMap specific, MultiMap general) {
		MultiValueMap result = new MultiValueMap();
		MultiMapUtil.copyMultiMap(result, specific);
		
		if (general != null) {
			for (Iterator<?> iter = general.keySet().iterator();iter.hasNext();) {
				Object key = iter.next();
	
				if (!specific.containsKey(key) ) {
					// inheriting a meta attribute only if it is inheritable
					Collection<?> ml = (Collection<?>)general.get(key);
					for (Iterator<?> iterator = ml.iterator(); iterator.hasNext();) {
						SimpleMetaAttribute element = (SimpleMetaAttribute) iterator.next();
						if (element.inheritable) {
							result.put(key, element);
						}
					}
				}
			}
		}
	
		return result;
	
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
	
	 static class SimpleMetaAttribute {
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
