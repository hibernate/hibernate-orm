/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh13058;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@Entity(name = "Patient")
@Table(name = "Patient")
public class Patient {

	@Id
	@GeneratedValue
	Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	Site site;

	public Patient() {
	}

	public Patient(Site site) {
		this.site = site;
	}

}
