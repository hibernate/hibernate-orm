/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.datareader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class TestDataReader {

	public List<TestDataElement> read(String fileName) {
		if ( fileName == null ) {
			throw new RuntimeException( "Null testsuite-suite data file specified." );
		}
		List<TestDataElement> testDataElements = new ArrayList<TestDataElement>();
		SAXReader reader = new SAXReader();
		// Make sure we use the "right" implementation as the Oracle XDB driver comes with a JAXP implementation as well
		SAXParserFactory factory = SAXParserFactory.newInstance( "org.apache.xerces.jaxp.SAXParserFactoryImpl", null );
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		try {
			factory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			factory.setFeature( "http://xml.org/sax/features/external-general-entities", false );
			factory.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );
			SAXParser parser = factory.newSAXParser();
			reader.setXMLReader( parser.getXMLReader() );
			Document document = reader.read( getInputStream( fileName ) );
			addDataElements( document, testDataElements );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
		return testDataElements;
	}

	protected void addDataElements(Document document, List<TestDataElement> testDataElements) {
		Element root = document.getRootElement();
		for ( Iterator it = root.elementIterator(); it.hasNext(); ) {
			Element element = (Element) it.next();
			addDataElement( element, testDataElements );
		}
	}

	protected void addDataElement(Element element, List<TestDataElement> testDataElements) {
		int id = Integer.parseInt( element.selectSingleNode( "id" ).getText() );
		String type = element.selectSingleNode( "type" ).getText();
		String wkt = element.selectSingleNode( "wkt" ).getText();
		TestDataElement testDataElement = new TestDataElement( id, type, wkt );
		testDataElements.add( testDataElement );
	}

	protected InputStream getInputStream(String fileName) {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( fileName );
		if ( is == null ) {
			throw new RuntimeException( String.format( "File %s not found on classpath.", fileName ) );
		}
		return is;
	}
}
