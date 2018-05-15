/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.embedded;

import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
public abstract class AbstractAddressable {
	private Address address;

	protected AbstractAddressable() {
	}

	protected AbstractAddressable(Address address) {
		this.address = address;
	}

	@Embedded
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}
