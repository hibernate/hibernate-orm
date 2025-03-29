/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.rawtypes;

import java.util.Collection;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class EmployeeWithRawType implements java.io.Serializable {

	@Id
	protected String id;

	@Basic
	protected String name;


	@ManyToMany(targetEntity = DeskWithRawType.class, mappedBy = "employees", cascade = CascadeType.ALL)
	protected Collection desks = new java.util.ArrayList();


	public EmployeeWithRawType() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection getDesks() {
		return desks;
	}

	public void setDesks(Collection desks) {
		this.desks = desks;
	}
}
