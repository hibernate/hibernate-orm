/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Pawel Stawicki
 */
@Entity
@DiscriminatorValue("1")
public class InheritingEntity extends ParentEntity {
	public InheritingEntity() {
	}

	public InheritingEntity(String someValue) {
		this.someValue = someValue;
	}

	@Column(name = "dupa")
	private String someValue;

	public String getSomeValue() {
		return someValue;
	}

	public void setSomeValue(String someValue) {
		this.someValue = someValue;
	}
}
