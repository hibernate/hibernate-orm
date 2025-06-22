/*
 * SPDX-License-Identifier: Apache-2.0
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
