/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddedid.withinheritance;

import java.io.Serializable;

/**
 * @author Hardy Ferentschik
 */
public abstract class AbstractRef implements Serializable {
	private final int id;

	protected AbstractRef() {
		// required by JPA
		id = 0;
	}

	protected AbstractRef(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
