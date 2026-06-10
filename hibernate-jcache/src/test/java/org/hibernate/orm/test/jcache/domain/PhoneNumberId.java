/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;

import java.io.Serializable;
import java.util.Objects;

public class PhoneNumberId implements Serializable {
	private long personId;
	private String numberType;

	public PhoneNumberId() {
	}

	public PhoneNumberId(long personId, String numberType) {
		this.personId = personId;
		this.numberType = numberType;
	}

	public long getPersonId() {
		return personId;
	}

	public void setPersonId(long personId) {
		this.personId = personId;
	}

	public String getNumberType() {
		return numberType;
	}

	public void setNumberType(String numberType) {
		this.numberType = numberType;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PhoneNumberId that = (PhoneNumberId) o;
		return personId == that.personId && Objects.equals( numberType, that.numberType );
	}

	@Override
	public int hashCode() {
		return Objects.hash( personId, numberType );
	}
}
