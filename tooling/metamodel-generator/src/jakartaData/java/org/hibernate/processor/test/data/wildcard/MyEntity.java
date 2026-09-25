/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.wildcard;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "MyWildcardEntity")
public class MyEntity {
	@Id
	Long id;
	String name;
}
