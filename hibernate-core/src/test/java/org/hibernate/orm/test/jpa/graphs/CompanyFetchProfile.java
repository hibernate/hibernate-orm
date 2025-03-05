/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Nathan Xu
 */
@Entity
@FetchProfile(
		name = "company.location",
		fetchOverrides = {
				@FetchProfile.FetchOverride(
						entity = CompanyFetchProfile.class,
						association = "location",
						mode = FetchMode.JOIN
				)
		}
)
public class CompanyFetchProfile {
	@Id @GeneratedValue
	public long id;

	@OneToMany
	public Set<Employee> employees = new HashSet<Employee>();

	@OneToOne(fetch = FetchType.LAZY)
	public Location location;

	@ElementCollection
	public Set<Market> markets = new HashSet<Market>();

	@ElementCollection(fetch = FetchType.EAGER)
	@JoinTable(name= "companyfp_phonenos")
	public Set<String> phoneNumbers = new HashSet<String>();

	public Location getLocation() {
		return location;
	}
}
