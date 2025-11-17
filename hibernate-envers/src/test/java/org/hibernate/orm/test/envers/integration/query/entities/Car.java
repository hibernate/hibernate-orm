/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.entities;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Entity
@Audited
public class Car {

	@Id
	@GeneratedValue
	private Long id;

	private String make;
	@ManyToOne
	private Person owner;
	@ManyToMany
	private Set<Person> drivers = new HashSet<Person>();

	public Car() {

	}

	public Car(final String make) {
		this.make = make;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}

	public Set<Person> getDrivers() {
		return drivers;
	}

	public void setDrivers(Set<Person> drivers) {
		this.drivers = drivers;
	}

}
