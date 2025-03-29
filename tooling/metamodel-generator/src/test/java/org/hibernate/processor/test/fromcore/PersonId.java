/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

/**
 * @author Christian Beikov
 */
@Embeddable
public class PersonId implements Serializable {

	private String ssn;
	private String name;

	public PersonId() {
	}

	public PersonId(String ssn, String name) {
		this.ssn = ssn;
		this.name = name;
	}

	public String getSsn() {
		return ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 67 * hash + (this.ssn != null ? this.ssn.hashCode() : 0);
		hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
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
		final PersonId other = (PersonId) obj;
		if ((this.ssn == null) ? (other.ssn != null) : !this.ssn.equals(other.ssn)) {
			return false;
		}
		if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public void setName(String name) {
		this.name = name;
	}
}
