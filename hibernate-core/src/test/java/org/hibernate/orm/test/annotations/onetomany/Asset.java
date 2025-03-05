/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Asset implements Serializable {

	@Id
	@Column(name = "id_asset")
	private final Integer idAsset;

	@Id
	@Column(name = "id_test")
	private final Integer test;

	@ManyToOne(cascade = { CascadeType.ALL })
	@JoinColumn(nullable = false)
	private Employee employee;

	public Asset() {
		this.idAsset = 0;
		this.test = 1;
	}

	/**
	 * @param idAsset
	 */
	public Asset(Integer idAsset) {
		this.idAsset = idAsset;
		this.test = 1;
	}

	/**
	 * @return the id
	 */
	public Integer getIdAsset() {
		return idAsset;
	}

	/**
	 * @return the employee
	 */
	public Employee getEmployee() {
		return employee;
	}

	/**
	 * @param employee the employee to set
	 */
	public void setEmployee(Employee employee) {
		this.employee = employee;
	}
}
