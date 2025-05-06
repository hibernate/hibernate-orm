/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.GenericGenerator;

/**
 * A testing entity for cases where the entity definition itself is irrelevant (testing JDBC connection semantics, etc).
 *
 * @author Shawn Clowater
 */
@Entity
public class IrrelevantEntity {
	private Integer id;
	private String name;

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NotBlank
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
