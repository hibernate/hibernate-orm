/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.tableperclass;

import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Robot extends Machine {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
