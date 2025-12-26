/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Access(AccessType.FIELD)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER)
@DiscriminatorValue("0")
public class Client {
	private Integer id;

	@Column(name = "CUSTOMER_NAME")
	private String name;

	@Id
	@Access(AccessType.PROPERTY)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
