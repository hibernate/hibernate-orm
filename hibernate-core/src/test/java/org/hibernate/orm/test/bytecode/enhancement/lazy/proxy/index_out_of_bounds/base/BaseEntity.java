/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.index_out_of_bounds.base;

import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {

	private Long id;

	protected EmbeddableType embeddedField;

	private Long dummyField;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@Embedded
	public EmbeddableType getEmbeddedField() {
		return embeddedField;
	}

	public void setEmbeddedField(final EmbeddableType embeddedField) {
		this.embeddedField = embeddedField;
	}

	public Long getDummyField() {
		return dummyField;
	}

	void setDummyField(Long dummyField) {
		this.dummyField = dummyField;
	}
}
