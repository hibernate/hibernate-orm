/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.multivaluebinding;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "#getPostsByName", query = "from Post p where p.name in (:names)")
public class Post {
	@Id
	Integer id;

	String name;

	Integer value;
}
