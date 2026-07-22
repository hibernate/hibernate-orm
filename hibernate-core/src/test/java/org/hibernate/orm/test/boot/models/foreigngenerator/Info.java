/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.foreigngenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * @author Steve Ebersole
 */
@Entity
public class Info {
	@Id
	private Integer id;
	@Basic
	private String name;

	@MapsId
	@ManyToOne
	private Thing owner;

	protected Info() {
		// for Hibernate use
	}

	public Info(Thing owner, String name) {
		this.owner = owner;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Thing getOwner() {
		return owner;
	}

	public void setOwner(Thing owner) {
		this.owner = owner;
	}
}
