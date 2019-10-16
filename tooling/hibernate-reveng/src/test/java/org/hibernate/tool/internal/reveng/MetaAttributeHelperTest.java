package org.hibernate.tool.internal.reveng;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections4.MultiValuedMap;
import org.hibernate.tool.internal.reveng.MetaAttributeHelper.SimpleMetaAttribute;
import org.junit.Assert;
import org.junit.Test;
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
		Assert.assertEquals(1, mm.size());
		Collection<SimpleMetaAttribute> attributeList = mm.get("blah");
		Assert.assertEquals(1, attributeList.size());
		Optional<SimpleMetaAttribute> first = attributeList.stream().findFirst();
		Assert.assertTrue(first.isPresent());
		SimpleMetaAttribute attribute = first.get();
		Assert.assertEquals(true, attribute.inheritable);
		Assert.assertEquals("foo", attribute.value);
	}

}
