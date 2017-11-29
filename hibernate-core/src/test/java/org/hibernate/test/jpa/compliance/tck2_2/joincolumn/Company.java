/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2.joincolumn;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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
