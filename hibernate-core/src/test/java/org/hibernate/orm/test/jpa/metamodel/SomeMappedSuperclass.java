/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
public class SomeMappedSuperclass {
	private Long id;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
