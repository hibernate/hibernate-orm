/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class ProgramManager {
	int id;

	Collection<Employee> manages;

	@Id
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToMany( mappedBy="jobInfo.pm", cascade= CascadeType.ALL )
	public Collection<Employee> getManages() {
		return manages;
	}

	public void setManages( Collection<Employee> manages ) {
		this.manages = manages;
	}

}
