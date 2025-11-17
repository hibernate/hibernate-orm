/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import java.util.Set;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Access;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
@Access(jakarta.persistence.AccessType.PROPERTY)
public class Address {
	private String street1;
	private String city;
	private Country country;
	private Set<Inhabitant> inhabitants;

	public String getStreet1() {
		return street1;
	}

	public void setStreet1(String street1) {
		this.street1 = street1;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@ElementCollection
	@CollectionTable(name = "Add_Inh")
	public Set<Inhabitant> getInhabitants() {
		return inhabitants;
	}

	public void setInhabitants(Set<Inhabitant> inhabitants) {
		this.inhabitants = inhabitants;
	}
}
