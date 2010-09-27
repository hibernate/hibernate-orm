/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $Id$

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
