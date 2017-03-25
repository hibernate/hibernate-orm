/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class TestDataReader {

	public List<TestDataElement> read(String fileName) {
		if ( fileName == null ) {
			throw new RuntimeException( "Null testsuite-suite data file specified." );
		}
		List<TestDataElement> testDataElements = new ArrayList<TestDataElement>();
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read( getInputStream( fileName ) );
			addDataElements( document, testDataElements );
		}
		catch (DocumentException e) {
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
