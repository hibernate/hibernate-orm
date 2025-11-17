/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SerialNumber {
	private SerialNumberPk id;
	private String value;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !(o instanceof SerialNumber serialNumber) ) return false;

		return id.equals( serialNumber.id );
	}

	public int hashCode() {
		return id.hashCode();
	}

	@Id
	public SerialNumberPk getId() {
		return id;
	}

	public void setId(SerialNumberPk id) {
		this.id = id;
	}

	@Column(name="`value`")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
