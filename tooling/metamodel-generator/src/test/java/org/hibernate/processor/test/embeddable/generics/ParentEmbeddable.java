/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.generics;

import java.util.Set;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class ParentEmbeddable<MyType extends MyTypeInterface> {
	private Set<MyType> fields;

	public Set<MyType> getFields() {
		return fields;
	}

	public void setFields(Set<MyType> fields) {
		this.fields = fields;
	}
}
