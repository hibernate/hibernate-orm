/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

/**
 * Test case for NaturalId annotation - ANN-750
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Entity
@NaturalIdCache
class NaturalIdOnManyToOne {

	@Id
	@GeneratedValue
	int id;

	@NaturalId
	@ManyToOne
	Citizen citizen;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Citizen getCitizen() {
		return citizen;
	}

	public void setCitizen(Citizen citizen) {
		this.citizen = citizen;
	}
}
