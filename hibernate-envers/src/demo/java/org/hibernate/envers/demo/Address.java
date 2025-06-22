/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.demo;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class Address {
	@Id
	@GeneratedValue
	private int id;

	@Audited
	private String streetName;

	@Audited
	private Integer houseNumber;

	@Audited
	private Integer flatNumber;

	@Audited
	@OneToMany(mappedBy = "address")
	private Set<Person> persons;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getStreetName() {
		return streetName;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	public Integer getHouseNumber() {
		return houseNumber;
	}

	public void setHouseNumber(Integer houseNumber) {
		this.houseNumber = houseNumber;
	}

	public Integer getFlatNumber() {
		return flatNumber;
	}

	public void setFlatNumber(Integer flatNumber) {
		this.flatNumber = flatNumber;
	}

	public Set<Person> getPersons() {
		return persons;
	}

	public void setPersons(Set<Person> persons) {
		this.persons = persons;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Address)) return false;

		Address address = (Address) o;

		if (id != address.id) return false;
		if (flatNumber != null ? !flatNumber.equals(address.flatNumber) : address.flatNumber != null) return false;
		if (houseNumber != null ? !houseNumber.equals(address.houseNumber) : address.houseNumber != null) return false;
		if (streetName != null ? !streetName.equals(address.streetName) : address.streetName != null) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = id;
		result = 31 * result + (streetName != null ? streetName.hashCode() : 0);
		result = 31 * result + (houseNumber != null ? houseNumber.hashCode() : 0);
		result = 31 * result + (flatNumber != null ? flatNumber.hashCode() : 0);
		return result;
	}
}
