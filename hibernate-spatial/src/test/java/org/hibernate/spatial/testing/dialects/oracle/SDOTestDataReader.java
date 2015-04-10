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
