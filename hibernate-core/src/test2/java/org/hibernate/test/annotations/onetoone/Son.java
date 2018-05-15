/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.test.annotations.onetoone;


/**
 * @author Hardy Ferentschik
 */
public class Son {
		private int id;

	private String name;

	private Father father;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Father getFather() {
		return father;
	}

	public void setFather(Father father) {
		this.father = father;
	}
}


