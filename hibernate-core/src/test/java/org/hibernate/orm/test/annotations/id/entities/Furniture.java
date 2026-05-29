/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.IncrementGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Furniture {
	private Integer id;

	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
