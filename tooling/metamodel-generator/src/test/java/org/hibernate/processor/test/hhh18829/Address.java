/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh18829;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class Address {
	String address;
}
