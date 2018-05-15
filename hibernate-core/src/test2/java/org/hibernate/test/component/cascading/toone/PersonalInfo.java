/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.cascading.toone;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PersonalInfo {
	private Address homeAddress = new Address();

	public PersonalInfo() {
	}

	public PersonalInfo(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

	public Address getHomeAddress() {
		return homeAddress;
	}

	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}
}
