/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base;

import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {

	@Id
	@GeneratedValue
	private Long id;

	@Embedded
	protected EmbeddableType embeddedField;

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public EmbeddableType getEmbeddedField() {
		return embeddedField;
	}

	public void setEmbeddedField(final EmbeddableType embeddedField) {
		this.embeddedField = embeddedField;
	}
}
