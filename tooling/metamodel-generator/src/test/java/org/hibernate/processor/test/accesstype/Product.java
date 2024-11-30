/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import java.math.BigDecimal;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@Entity
public class Product {

	transient String nonPersistent;
	static String nonPersistent2;

	@Id
	long id;

	int test;

	String description;
	BigDecimal price;

	@ManyToOne
	Shop shop;

	@OneToMany
	Set<Item> items;
}
