/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

/**
 * @author Koen Aers
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfArrays {

	private Integer id;
	private String name;

	private String[] arrayOfBasics;


	public EntityOfArrays() {
	}

	public EntityOfArrays(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// arrayOfBasics

	@ElementCollection
	@OrderColumn
	public String[] getArrayOfBasics() {
		return arrayOfBasics;
	}

	public void setArrayOfBasics(String[] arrayOfBasics) {
		this.arrayOfBasics = arrayOfBasics;
	}

}
