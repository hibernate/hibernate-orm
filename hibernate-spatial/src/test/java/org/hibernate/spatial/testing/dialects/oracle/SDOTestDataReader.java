/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.oracle;

import java.util.List;

import org.dom4j.Element;

import org.hibernate.spatial.testing.TestDataElement;
import org.hibernate.spatial.testing.TestDataReader;


public class SDOTestDataReader extends TestDataReader {


	@Override
	protected void addDataElement(Element element, List<TestDataElement> testDataElements) {
		int id = Integer.parseInt( element.selectSingleNode( "id" ).getText() );
		String type = element.selectSingleNode( "type" ).getText();
		String wkt = element.selectSingleNode( "wkt" ).getText();
		String sdo = element.selectSingleNode( "sdo" ).getText();
		TestDataElement testDataElement = new SDOTestDataElement( id, type, wkt, sdo );
		testDataElements.add( testDataElement );
	}

}
