/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
		catch ( DocumentException e ) {
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
		TestDataElement testDataElement = new TestDataElement( id, type, wkt);
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