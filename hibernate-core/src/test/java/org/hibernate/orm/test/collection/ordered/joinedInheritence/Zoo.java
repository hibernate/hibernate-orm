/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.ordered.joinedInheritence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLOrder;

/**
 * @author Steve Ebersole
 */
@Entity
public class Zoo {
	private Long id;
	private String name;
	private String city;
	private Set<Tiger> tigers = new HashSet<>();
	private Set<Lion> lions = new HashSet<>();
	private Set<Animal> animals = new HashSet<>();

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( strategy = "increment", name = "increment" )
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

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@OneToMany
	@JoinColumn
	@jakarta.persistence.OrderBy( "details" )
	public Set<Tiger> getTigers() {
		return tigers;
	}

	public void setTigers(Set<Tiger> tigers) {
		this.tigers = tigers;
	}

	@OneToMany
	@JoinColumn
	@SQLOrder( "weight" )
	public Set<Lion> getLions() {
		return lions;
	}

	public void setLions(Set<Lion> lions) {
		this.lions = lions;
	}

	@OneToMany
	@JoinColumn
	@jakarta.persistence.OrderBy( "id asc" ) // HHH-7630 ensure explicitly naming the superclass id works
	public Set<Animal> getAnimalsById() {
		return animals;
	}

	public void setAnimalsById(Set<Animal> animals) {
		this.animals = animals;
	}
}
