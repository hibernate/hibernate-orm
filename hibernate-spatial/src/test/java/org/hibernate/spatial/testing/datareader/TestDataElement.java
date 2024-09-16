/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.datareader;

/**
 * A <code>TestDataElement</code> captures the information necessary to build a testsuite-suite geometry.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class TestDataElement {


	final public String wkt;
	final public int id;
	final public String type;

	protected TestDataElement(int id, String type, String wkt) {
		this.wkt = wkt;
		this.id = id;
		this.type = type;
	}

}
