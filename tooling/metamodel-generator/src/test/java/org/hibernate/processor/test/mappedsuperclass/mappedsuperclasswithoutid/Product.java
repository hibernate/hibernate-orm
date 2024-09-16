/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.mappedsuperclasswithoutid;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.processor.test.accesstype.Shop;

/**
 * @author Hardy Ferentschik
 */
@MappedSuperclass
public class Product {
	@ManyToOne
	Shop shop;
}
