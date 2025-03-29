/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
class PersonPair implements Serializable {

	private static final long serialVersionUID = 4543565503074112720L;

	@ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "LEFT_PERSON_ID", nullable = false, updatable = false,
			foreignKey = @ForeignKey(name = "FK_LEFT_PERSON"))
	private Person left;

	@ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "RIGHT_PERSON_ID", nullable = false, updatable = false,
			foreignKey = @ForeignKey(name = "FK_RIGHT_PERSON"))
	private Person right;

	PersonPair() {
		super();
	}

	PersonPair(final Person pLeft, final Person pRight) {
		super();
		this.left = pLeft;
		this.right = pRight;
	}

	Person getLeft() {
		return this.left;
	}

	Person getRight() {
		return this.right;
	}

	@Override
	public int hashCode() {
		final int prime = 107;
		int result = 1;
		result = prime * result + ((getLeft() == null) ? 0 : getLeft().hashCode());
		result = prime * result + ((getRight() == null) ? 0 : getRight().hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object pObject) {
		if (this == pObject) {
			return true;
		}
		if (pObject == null) {
			return false;
		}
		if (!(pObject instanceof PersonPair)) {
			return false;
		}
		final PersonPair other = (PersonPair) pObject;
		if (getRight() == null) {
			if (other.getRight() != null) {
				return false;
			}
		} else if (!getRight().equals(other.getRight())) {
			return false;
		}
		if (getLeft() == null) {
			if (other.getLeft() != null) {
				return false;
			}
		} else if (!getLeft().equals(other.getLeft())) {
			return false;
		}
		return true;
	}

}
