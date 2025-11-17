/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PoliticalParty {
	private String name;
	private Set<Politician> politicians = new HashSet<>();

	@Id
	@Column(columnDefinition = "VARCHAR(60)")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
	public Set<Politician> getPoliticians() {
		return politicians;
	}

	public void setPoliticians(Set<Politician> politicians) {
		this.politicians = politicians;
	}

	public void addPolitician(Politician politician) {
		politicians.add( politician );
		politician.setParty( this );
	}
}
