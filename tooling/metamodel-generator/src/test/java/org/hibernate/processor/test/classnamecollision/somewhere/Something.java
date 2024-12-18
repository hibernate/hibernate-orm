/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.classnamecollision.somewhere;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
abstract public class Something {
	String name;
}
