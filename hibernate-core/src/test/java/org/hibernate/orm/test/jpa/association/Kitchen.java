/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.association;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Kitchen {
	@Id
	@GeneratedValue
	private Long id;

	@OneToOne(mappedBy = "kitchen")
	private Oven oven;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Oven getOven() {
		return oven;
	}

	public void setOven(Oven oven) {
		this.oven = oven;
	}
}
