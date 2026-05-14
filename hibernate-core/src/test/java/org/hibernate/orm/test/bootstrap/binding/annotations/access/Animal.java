/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Yanming Zhou
 */
@MappedSuperclass
public class Animal {

	private Long id;

	@Id
	@Access(AccessType.FIELD)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
