/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity without any JPA lifecycle callback methods
 */
@Entity
public class PlainEntity {
	@Id
	private Integer id;

	private String name;
}
