/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedentity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity(name = "Liber")
@NamedQuery(name = "findAllBooks", query = "from Liber")
public class Book {
	@Id
	private Integer id;

	private String name;
}
