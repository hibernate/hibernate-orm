/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.classnamecollision.somewhere;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
abstract public class Something {
	String name;
}
