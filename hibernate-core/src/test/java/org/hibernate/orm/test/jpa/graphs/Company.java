/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;


/**
 * @author Brett Meyer
 */
@Entity
public class Company {
	@Id @GeneratedValue
	public long id;

	@OneToMany
	public Set<Employee> employees = new HashSet<Employee>();

	@OneToOne(fetch = FetchType.LAZY)
	public Location location;

	@ElementCollection
	public Set<Market> markets = new HashSet<Market>();

	@ElementCollection(fetch = FetchType.EAGER)
	public Set<String> phoneNumbers = new HashSet<String>();

	public Location getLocation() {
		return location;
	}
}
