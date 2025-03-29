/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;


public class Thing {
	private String description;
	private Person person;
	private int version;
	private String longDescription;

	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	Thing() {}
	public Thing(String description, Person person) {
		this.description = description;
		this.person = person;
		person.getThings().add(this);
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	public String getLongDescription() {
		return longDescription;
	}
	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}
}
