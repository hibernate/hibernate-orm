/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Name extends FirstName {

	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	private LastName lastName;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LastName getLastName() {
		return lastName;
	}

	public void setLastName(LastName val) {
		this.lastName = val;
	}

}
