/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

/**
 * Entity with a JPA lifecycle callback method directly on the entity class
 */
@Entity
public class EntityWithCallback {
	@Id
	private Integer id;

	private String name;

	@PrePersist
	public void prePersist() {
	}
}
