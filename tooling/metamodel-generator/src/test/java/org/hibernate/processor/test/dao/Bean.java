/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Bean {
	@Id
	public Long getKey() {
		return 69L;
	}
	public String getText() {
		return "Hello World";
	}
}
