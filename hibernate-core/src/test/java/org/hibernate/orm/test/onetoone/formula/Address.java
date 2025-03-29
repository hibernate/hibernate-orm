/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.formula;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Address implements Serializable {
	private Person person;
	private String type;
	private String street;
	private String state;
	private String zip;

	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}

	public boolean equals(Object that) {
		if ( !(that instanceof Address) ) return false;
		Address address = (Address) that;
		return address.getType().equals(type) &&
			address.getPerson().getName().equals( person.getName() );
	}

	public int hashCode() {
		return person.getName().hashCode() + type.hashCode();
	}
}
