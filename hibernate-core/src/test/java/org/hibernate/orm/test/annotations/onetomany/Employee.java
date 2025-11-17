/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Employee {

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "employee", orphanRemoval = true)
	@OrderBy // order by PK
	private final List<Asset> assets = new ArrayList<Asset>();

	@Id
	@Column(name = "id")
	private Integer id;

	public Employee() {

	}

	/**
	 * @param id
	 */
	public Employee(Integer id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the assets
	 */
	public List<Asset> getAssets() {
		return assets;
	}
}
