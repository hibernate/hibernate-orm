/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Daniël van Eeden
 */
@Entity
@Table(name = "names")
public class Names {
	@Id
	private Integer id;
	private String name;

	public Names() {
	}

	public Names(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}
