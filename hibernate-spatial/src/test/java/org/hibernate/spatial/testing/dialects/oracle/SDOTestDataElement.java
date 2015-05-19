/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.oracle;


import org.hibernate.spatial.testing.TestDataElement;

/**
 * A specialised subclass for SDOGeometry test objects
 * <p/>
 * Oracle 10g WKT support is limited to 2D geometries, and there is
 * no method of specifying SRID. That is why we here add the equivalent SDO expression
 * that can be used by the TestData
 */
public class SDOTestDataElement extends TestDataElement {

	public final String sdo;

	public SDOTestDataElement(int id, String type, String wkt, String sdo) {
		super( id, type, wkt);
		this.sdo = sdo;
	}

}
