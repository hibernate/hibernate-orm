/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.join;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToOne;

/**
 *
 * @author Steve Ebersole
 */
@Entity
public class Person {
	@Id
	private Integer id;
	@Basic
	private String name;
	private String stuff;
	@ManyToOne
	private SupplementalDetails details;
	@Embedded
	private Data data;
	private String datum;

	protected Person() {
		// for Hibernate use
	}

	public Person(Integer id, String name) {
		this.id = id;
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
}
