/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2.joincolumn;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Steve Ebersole
 */
@Entity
public class Company {
	private Integer id;
	private String name;
	private Set<Location> locations;

	public Company() {
	}

	public Company(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public void addLocation(Location location) {
		if ( locations == null ) {
			locations = new HashSet<>();
		}

		locations.add( location );
		location.setCompany( this );
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(cascade = CascadeType.PERSIST, mappedBy = "company")
	public Set<Location> getLocations() {
		return locations == null ? Collections.emptySet() : locations;
	}

	public void setLocations(Set<Location> locations) {
		this.locations = locations;
	}
}
