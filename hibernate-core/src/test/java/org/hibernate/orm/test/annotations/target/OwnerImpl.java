/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.target;

import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class OwnerImpl implements Owner {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
