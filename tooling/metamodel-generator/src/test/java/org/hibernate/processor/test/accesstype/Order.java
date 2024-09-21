/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
//@Entity
public class Order {

	//@Id
	long id;

	//@OneToMany
	Set<Item> items;

	boolean filled;
	Date date;

	//@OneToMany
	List<String> notes;

	//@ManyToOne
	Shop shop;
}
