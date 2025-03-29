/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.embeddedid;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Person implements Serializable {
	@Id
	@GeneratedValue
	private long id;

	private String name;

	@OneToMany(mappedBy = "personA")
	private Set<PersonTuple> personATuples = new HashSet<PersonTuple>();

	@OneToMany(mappedBy = "personB")
	private Set<PersonTuple> personBTuples = new HashSet<PersonTuple>();

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}

	public Person(long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Person) ) {
			return false;
		}

		Person person = (Person) o;

		if ( id != person.id ) {
			return false;
		}
		if ( name != null ? !name.equals( person.name ) : person.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Person(id = " + id + ", name = " + name + ")";
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<PersonTuple> getPersonBTuples() {
		return personBTuples;
	}

	public void setPersonBTuples(Set<PersonTuple> personBTuples) {
		this.personBTuples = personBTuples;
	}

	public Set<PersonTuple> getPersonATuples() {
		return personATuples;
	}

	public void setPersonATuples(Set<PersonTuple> personATuples) {
		this.personATuples = personATuples;
	}
}
