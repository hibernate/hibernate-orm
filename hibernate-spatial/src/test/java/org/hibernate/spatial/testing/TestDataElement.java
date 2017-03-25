/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

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
