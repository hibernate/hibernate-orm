/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query.entities;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

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
