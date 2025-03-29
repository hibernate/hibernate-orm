/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.lazytoone.collectioninitializer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Company {
	@Id
	private Long id;

	@Override
	public String toString() {
		return "Company{" +
				"id=" + id +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
