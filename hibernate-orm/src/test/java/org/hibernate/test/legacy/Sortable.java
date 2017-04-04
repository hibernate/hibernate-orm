/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


public class Sortable implements Comparable {
	
	private int id;
	private String name;
	
	private Sortable() {}
	Sortable(String name) {
		this.name = name;
	}
	
	public int compareTo(Object o) {
		return name.compareTo( ( (Sortable) o).name );
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(int i) {
		id = i;
	}

	public void setName(String string) {
		name = string;
	}

}
