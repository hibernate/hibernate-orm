/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metadata;


/**
 * @author Emmanuel Bernard
 */
//not an entity but in between mapped superclass and entity
public class SubThing extends Thing {
	private String blah;

	public String getBlah() {
		return blah;
	}

	public void setBlah(String blah) {
		this.blah = blah;
	}
}
