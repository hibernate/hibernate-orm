/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PreRemove;

/**
 * Annotated JPA entity listener (not declared via XML)
 */
public class AnnotatedEntityListener {
	@PostPersist
	public void entityCreated(Object entity) {
	}

	@PreRemove
	public void entityRemoved(Object entity) {
	}
}
