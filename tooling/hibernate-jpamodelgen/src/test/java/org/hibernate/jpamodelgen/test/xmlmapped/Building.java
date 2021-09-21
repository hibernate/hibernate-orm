/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.xmlmapped;

import org.hibernate.jpamodelgen.test.accesstype.Address;
import org.hibernate.jpamodelgen.test.accesstype.Area;

/**
 * @author Hardy Ferentschik
 */
public class Building extends Area {
	private Address address;

	public Address getAddress() {
		return address;
	}

	public void setAddress(org.hibernate.jpamodelgen.test.accesstype.Address address) {
		this.address = address;
	}
}
