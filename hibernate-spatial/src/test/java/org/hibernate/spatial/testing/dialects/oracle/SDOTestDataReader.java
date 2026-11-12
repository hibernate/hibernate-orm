package org.hibernate.spatial.testing.dialects.oracle;

import org.hibernate.spatial.testing.datareader.TestDataReader;

import org.w3c.dom.Element;

public class SDOTestDataReader extends TestDataReader {

	@Override
	protected SDOTestDataElement readElement(Element element) {
		return new SDOTestDataElement(
				Integer.parseInt( childText( element, "id" ) ),
				childText( element, "type" ),
				childText( element, "wkt" ),
				childText( element, "sdo" )
		);
	}
}
