/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
@EntityListeners( AnimalWatcher.class )
public class Dog implements Animal {
	@Id
	private Integer id;
	private String name;

	public Dog() {
	}

	public Dog(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}
