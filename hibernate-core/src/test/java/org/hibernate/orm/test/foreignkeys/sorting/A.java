/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.foreignkeys.sorting;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class A {
	@Id
	@GeneratedValue
	private int id;

	@ManyToOne(cascade = CascadeType.PERSIST)
	B b;

	public A() {
	}

	public A(B b) {
		this.b = b;
	}
}
