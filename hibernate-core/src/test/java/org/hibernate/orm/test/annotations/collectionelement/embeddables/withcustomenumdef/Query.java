/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.embeddables.withcustomenumdef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name="`Query`")
public class Query {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	private Long id;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<Location> includedLocations = new HashSet<Location>();

	public Query() {
	}

	public Query(Location... locations) {
		Collections.addAll( includedLocations, locations );
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Location> getIncludedLocations() {
		return includedLocations;
	}

	public void setIncludedLocations(Set<Location> includedLocations) {
		this.includedLocations = includedLocations;
	}
}
