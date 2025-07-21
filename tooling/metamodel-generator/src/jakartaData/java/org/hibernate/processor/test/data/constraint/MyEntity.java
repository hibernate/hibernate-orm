/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.constraint;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyEntity {

	@Id
	private Long id;
	private String name;
}
