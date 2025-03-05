/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;

/**
 * @author Christian Beikov
 */
@MappedSuperclass
public abstract class BaseEmbeddedEntity<I extends Serializable> implements Serializable {

	private I id;

	public BaseEmbeddedEntity() {
	}

	public BaseEmbeddedEntity(I id) {
		this.id = id;
	}

	@EmbeddedId
	public I getId() {
		return id;
	}

	public void setId(I id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 47 * hash + (this.id != null ? this.id.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final BaseEmbeddedEntity<?> other = (BaseEmbeddedEntity<?>) obj;
		if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
			return false;
		}
		return true;
	}
}
