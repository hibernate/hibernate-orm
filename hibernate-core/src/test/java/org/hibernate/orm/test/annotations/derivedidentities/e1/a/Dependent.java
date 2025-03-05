/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.a;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Cascade;


/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(DependentId.class)
public class Dependent {
	private String name;
	// id attribute mapped by join column default
	private Employee emp;

	public Dependent() {
	}

	public Dependent(String name, Employee emp) {
		this.name = name;
		this.emp = emp;
	}

	@Id
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
	@ManyToOne( cascade = CascadeType.PERSIST )
	@Cascade( {org.hibernate.annotations.CascadeType.PERSIST, org.hibernate.annotations.CascadeType.MERGE} )
	@JoinColumn(nullable=false)
	public Employee getEmp() {
		return emp;
	}

	public void setEmp(Employee emp) {
		this.emp = emp;
	}
}
