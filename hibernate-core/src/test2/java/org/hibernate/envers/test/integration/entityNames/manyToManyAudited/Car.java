/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.entityNames.manyToManyAudited;

import java.util.List;

import org.hibernate.envers.Audited;

/**
 * @author Hern&aacute;n Chanfreau
 */

@Audited
public class Car {

	private long id;

	private int registrationNumber;

	private List<Person> owners;


	public Car() {
	}

	public Car(int registrationNumber, List<Person> owners) {
		this.registrationNumber = registrationNumber;
		this.owners = owners;
	}


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<Person> getOwners() {
		return owners;
	}

	public void setOwners(List<Person> owners) {
		this.owners = owners;
	}

	public int getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(int registrationNumber) {
		this.registrationNumber = registrationNumber;
	}


}
