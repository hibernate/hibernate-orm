/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.xmlonly;

public class Tire {
	public Long getId() {
		return 1L;
	}

	public void setId(Long id) {
	}

	public Car getCar() {
		return null;
	}

	public void setCar(Car car) {
	}
}

