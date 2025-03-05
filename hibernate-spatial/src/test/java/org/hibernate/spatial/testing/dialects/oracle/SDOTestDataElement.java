/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.oracle;


import org.hibernate.spatial.testing.datareader.TestDataElement;

/**
 * A specialised subclass for SDOGeometry test objects
 * <p>
 * Oracle 10g WKT support is limited to 2D geometries, and there is
 * no method of specifying SRID. That is why we here add the equivalent SDO expression
 * that can be used by the TestData
 */
public class SDOTestDataElement extends TestDataElement {

	public final String sdo;

	public SDOTestDataElement(int id, String type, String wkt, String sdo) {
		super( id, type, wkt );
		this.sdo = sdo;
	}

}
