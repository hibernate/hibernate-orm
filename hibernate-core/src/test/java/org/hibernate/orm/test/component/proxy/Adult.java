/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.proxy;

import jakarta.persistence.Entity;

@Entity
public class Adult extends Person {

	public Adult() {
		someInitMethod();
	}

	@Override
	public void someInitMethod() {
	}
}
