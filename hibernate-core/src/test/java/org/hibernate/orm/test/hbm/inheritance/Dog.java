/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hbm.inheritance;

public class Dog extends Animal {

	private DogName name;

	public DogName getName() {
		return name;
	}

	public void setName(DogName name) {
		this.name = name;
	}
}
