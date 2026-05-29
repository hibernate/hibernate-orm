/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.reversed.bidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Employee {
	@Id
	@GeneratedValue
	private Long id;
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "info_id", unique = true)
	private EmployeeInfo info;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EmployeeInfo getInfo() {
		return info;
	}

	public void setInfo(EmployeeInfo info) {
		this.info = info;
	}
}
