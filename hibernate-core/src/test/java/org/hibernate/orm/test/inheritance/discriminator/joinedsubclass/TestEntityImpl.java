/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.Id;

/**
 * @author Andrea Boriero
 */
@jakarta.persistence.Entity
public class TestEntityImpl implements TestEntityInterface {
	@Id
	private Integer id;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}
}
