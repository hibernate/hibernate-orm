/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hqlfetchscroll;

public class Child {

	// A numeric id must be the <id> field.  Some databases (Sybase, etc.)
	// require identifier columns in order to support scrollable results.
	private long id;
	private String name;

	Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
