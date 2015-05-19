/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mixedmode;

/**
 * @author Hardy Ferentschik
 */
public class Car extends Vehicle {
	private String make;

	public int getHorsePower() {
		return 0;
	}

	public void setHorsePower(int horsePower) {
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}
}



