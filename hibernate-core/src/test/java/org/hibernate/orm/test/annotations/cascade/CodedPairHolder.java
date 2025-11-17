/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CODED_PAIR_HOLDER")
class CodedPairHolder implements Serializable {

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "CODE", nullable = false, unique = true, updatable = false, length = 256)
	private String code;

	private PersonPair pair;

	CodedPairHolder() {
		super();
	}

	CodedPairHolder(final String pCode, PersonPair pair) {
		super();
		this.code = pCode;
		this.pair = pair;
	}

	Long getId() {
		return this.id;
	}

	String getCode() {
		return this.code;
	}

	PersonPair getPair() {
		return this.pair;
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
		if (!(pObject instanceof CodedPairHolder )) {
			return false;
		}
		final CodedPairHolder other = (CodedPairHolder) pObject;
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
