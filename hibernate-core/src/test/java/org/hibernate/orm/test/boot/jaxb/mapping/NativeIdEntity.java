/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class NativeIdEntity {
	@Id
	private Integer id;

	private NativeIdEntity() {
	}

	public NativeIdEntity(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}
}
