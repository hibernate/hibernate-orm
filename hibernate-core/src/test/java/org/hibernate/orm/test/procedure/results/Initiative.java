/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure.results;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "initiatives")
public class Initiative {
	@Id
	private Integer id;
	private String name;
	@Column(name = "target_quarter")
	private String targetQuarter;

	protected Initiative() {
	}

	public Initiative(Integer id, String name, String targetQuarter) {
		this.id = id;
		this.name = name;
		this.targetQuarter = targetQuarter;
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

	public String getTargetQuarter() {
		return targetQuarter;
	}

	public void setTargetQuarter(String targetQuarter) {
		this.targetQuarter = targetQuarter;
	}
}
