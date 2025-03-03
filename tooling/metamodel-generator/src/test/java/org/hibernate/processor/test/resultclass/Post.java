/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.resultclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "#getNameValue", query = "select p.name, p.value from Post p", resultClass = NameValue.class)
public class Post {
	@Id
	Integer id;

	String name;

	Integer value;
}
