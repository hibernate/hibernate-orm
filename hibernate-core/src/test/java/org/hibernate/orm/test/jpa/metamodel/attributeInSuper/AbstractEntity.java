/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Hardy Ferentschik
 */
@MappedSuperclass
public class AbstractEntity {
	@Id
	private long id;

	@Embedded
	private EmbeddableEntity embedded;

	public long getId() {
		return id;
	}

	public EmbeddableEntity getEmbedded() {
		return embedded;
	}
}
