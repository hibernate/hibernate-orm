/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Stuff<Value> {
	private Value value;

	@ManyToOne
	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}
}
