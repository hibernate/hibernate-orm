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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "CODED_PAIR_SET_HOLDER")
class CodedPairSetHolder implements Serializable {

	private static final long serialVersionUID = 3757670856634990003L;

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "CODE", nullable = false, unique = true, updatable = false, length = 256)
	private String code;

	@ElementCollection
	@CollectionTable(name = "CODED_PAIR_HOLDER_PAIR_SET", joinColumns = @JoinColumn(name = "CODED_PAIR_HOLDER_ID"),
			foreignKey = @ForeignKey(name = "FK_PAIR_SET"))
	private final Set<PersonPair> pairs = new HashSet<PersonPair>(0);

	CodedPairSetHolder() {
		super();
	}

	CodedPairSetHolder(final String pCode, final Set<PersonPair> pPersonPairs) {
		super();
		this.code = pCode;
		this.pairs.addAll(pPersonPairs);
	}

	Long getId() {
		return this.id;
	}

	String getCode() {
		return this.code;
	}

	Set<PersonPair> getPairs() {
		return Collections.unmodifiableSet(this.pairs);
	}

	@Override
	public int hashCode() {
		final int prime = 101;
		int result = 1;
		result = prime * result + ((getCode() == null) ? 0 : getCode().hashCode());
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
		if (!(pObject instanceof CodedPairSetHolder )) {
			return false;
		}
		final CodedPairSetHolder other = (CodedPairSetHolder) pObject;
		if (getCode() == null) {
			if (other.getCode() != null) {
				return false;
			}
		} else if (!getCode().equals(other.getCode())) {
			return false;
		}
		return true;
	}

}
