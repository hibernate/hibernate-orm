/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.eg;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
public class Publisher {
	@Id long id;

	@Basic(optional = false)
	String name;

	@OneToMany(mappedBy = "publisher")
	Set<Book> books;
}
