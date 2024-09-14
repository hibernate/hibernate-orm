/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hbm.inheritance;

public class Cat extends Animal {

	private CatName name;

	public CatName getName() {
		return name;
	}

	public void setName(CatName name) {
		this.name = name;
	}
}
