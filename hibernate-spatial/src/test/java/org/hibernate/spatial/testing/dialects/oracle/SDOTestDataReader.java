/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.oracle;

import java.util.List;

import org.hibernate.spatial.testing.datareader.TestDataElement;
import org.hibernate.spatial.testing.datareader.TestDataReader;

import org.dom4j.Element;


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
