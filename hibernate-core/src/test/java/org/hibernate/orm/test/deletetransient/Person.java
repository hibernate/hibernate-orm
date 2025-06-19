/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletetransient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * todo: describe Person
 *
 * @author Steve Ebersole
 */
public class Person {
	private Long id;
	private String name;
	private Set<Address> addresses = new HashSet<>();
	private Collection<Person> friends = new ArrayList<>();

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}

	public Collection<Person> getFriends() {
		return friends;
	}

	public void setFriends(Collection<Person> friends) {
		this.friends = friends;
	}
}
