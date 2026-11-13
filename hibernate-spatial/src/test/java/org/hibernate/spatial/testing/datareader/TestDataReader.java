package org.hibernate.spatial.testing.datareader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

public class TestDataReader {

	public List<TestDataElement> read(String fileName) {
		if ( fileName == null ) {
			throw new RuntimeException( "Null testsuite-suite data file specified." );
		}
		// Use the JDK parser even when Oracle XDB supplies a JAXP provider.
		var factory = DocumentBuilderFactory.newDefaultNSInstance();
		try ( var input = getInputStream( fileName ) ) {
			factory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			factory.setFeature( "http://xml.org/sax/features/external-general-entities", false );
			factory.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );
			var root = factory.newDocumentBuilder().parse( input ).getDocumentElement();
			var testDataElements = new ArrayList<TestDataElement>();
			for ( var child = root.getFirstChild(); child != null; child = child.getNextSibling() ) {
				if ( child instanceof Element element ) {
					testDataElements.add( readElement( element ) );
				}
			}
			return testDataElements;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	protected TestDataElement readElement(Element element) {
		return new TestDataElement(
				Integer.parseInt( childText( element, "id" ) ),
				childText( element, "type" ),
				childText( element, "wkt" )
		);
	}

	protected static String childText(Element parent, String name) {
		for ( var child = parent.getFirstChild(); child != null; child = child.getNextSibling() ) {
			if ( child instanceof Element element
					&& element.getNamespaceURI() == null
					&& name.equals( element.getTagName() ) ) {
				return element.getTextContent();
			}
		}
		throw new IllegalArgumentException( "Missing <" + name + "> in <" + parent.getTagName() + ">" );
	}

	protected InputStream getInputStream(String fileName) {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( fileName );
		if ( is == null ) {
			throw new RuntimeException( String.format( "File %s not found on classpath.", fileName ) );
		}
		return is;
	}
}
