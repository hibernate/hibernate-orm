/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;

import jakarta.persistence.Entity;

/**
 * @author Gail Badner
 */
@Entity(name="HomoGigantus")
public class Giant extends Person {
	private long height;

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}
}
