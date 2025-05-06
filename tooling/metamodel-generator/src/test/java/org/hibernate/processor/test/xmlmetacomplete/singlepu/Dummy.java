/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlmetacomplete.singlepu;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Dummy {
	@Id
	private long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
