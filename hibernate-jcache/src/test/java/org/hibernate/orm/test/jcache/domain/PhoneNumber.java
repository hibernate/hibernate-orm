/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * PhoneNumber
 */
@Entity
@Table(name = "PHONE_NUMBERS")
@IdClass(PhoneNumberId.class)
public class PhoneNumber implements Serializable {
	@Id
	@Column(name = "PERSON_ID")
	private long personId = 0;

	@Id
	@Column(name = "NUMBER_TYPE")
	private String numberType = "home";

	@Column(name = "PHONE", precision = 22, scale = 0)
	private long phone = 0;

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

	public long getPhone() {
		return phone;
	}

	public void setPhone(long phone) {
		this.phone = phone;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
						+ ((numberType == null) ? 0 : numberType.hashCode());
		result = prime * result + (int)(personId ^ (personId >>> 32));
		result = prime * result + (int)(phone ^ (phone >>> 32));
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PhoneNumber other = (PhoneNumber)obj;
		if (numberType == null) {
			if (other.numberType != null)
				return false;
		} else if (!numberType.equals(other.numberType))
			return false;
		if (personId != other.personId)
			return false;
		if (phone != other.phone)
			return false;
		return true;
	}

	public String toString() {
		return numberType + ":" + phone;
	}

}
