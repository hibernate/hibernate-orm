package org.hibernate.tool.internal.reveng;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections.MultiMap;
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
		MultiMap mm = MetaAttributeHelper.loadMetaMap(document.getDocumentElement());
		Assert.assertEquals(1, mm.size());
		ArrayList<?> attributeList = (ArrayList<?>)mm.get("blah");
		Assert.assertEquals(1, attributeList.size());
		MetaAttributeHelper.SimpleMetaAttribute attribute = 
				(MetaAttributeHelper.SimpleMetaAttribute)attributeList.get(0);
		Assert.assertEquals(true, attribute.inheritable);
		Assert.assertEquals("foo", attribute.value);
	}

}
