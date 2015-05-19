/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.accesstype;

import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Access;
import javax.persistence.ElementCollection;
import javax.persistence.CollectionTable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
@Access(javax.persistence.AccessType.PROPERTY)
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
