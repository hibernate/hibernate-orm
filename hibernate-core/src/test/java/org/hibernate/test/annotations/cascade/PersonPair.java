/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.cascade;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
